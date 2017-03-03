/*
 * Copyright (c) 2016. Real Time Genomics Limited.
 *
 * Use of this source code is bound by the Real Time Genomics Limited Software Licence Agreement
 * for Academic Non-commercial Research Purposes only.
 *
 * If you did not receive a license accompanying this file, a copy must first be obtained by email
 * from support@realtimegenomics.com.  On downloading, using and/or continuing to use this source
 * code you accept the terms of that license agreement and any amendments to those terms that may
 * be made from time to time by Real Time Genomics Limited.
 */

package com.rtg.reader;

import static com.rtg.launcher.CommonFlags.NO_GZIP;
import static com.rtg.launcher.CommonFlags.OUTPUT_FLAG;
import static com.rtg.launcher.CommonFlags.QUALITY_FLAG;
import static com.rtg.reader.FastqTrim.BATCH_SIZE;
import static com.rtg.reader.Sdf2Fasta.INTERLEAVE;
import static com.rtg.util.cli.CommonFlagCategories.FILTERING;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Collections;
import java.util.function.Function;

import com.rtg.alignment.SingleIndelSeededEditDistance;
import com.rtg.alignment.UnidirectionalAdaptor;
import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.ngs.MapFlags;
import com.rtg.ngs.MapParamsHelper;
import com.rtg.ngs.NgsParams;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Validator;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.diagnostic.Timer;
import com.rtg.util.io.BaseFile;
import com.rtg.util.io.FileUtils;

/**
 */
public class PairedEndTrimCli extends AbstractCli {

  private static final String PETRIM_MODULE_NAME = "petrim";


  private static final String RIGHT = "right";
  private static final String LEFT = "left";

  private static final String MIN_IDENTITY = "min-overlap-identity";
  private static final String MIN_OVERLAP = "min-overlap-length";

  private static final String PROBE_LENGTH = "probe-length";


  @Override
  protected void initFlags() {
    mFlags.setDescription(description());
    initFlags(mFlags);
  }

  static void initFlags(CFlags flags) {
    flags.registerExtendedHelp();
    CommonFlagCategories.setCategories(flags);
    flags.registerRequired('l', LEFT, File.class, "FILE", "left input FASTQ file (AKA R1)").setCategory(INPUT_OUTPUT);
    flags.registerRequired('r', RIGHT, File.class, "FILE", "right input FASTQ file (AKA R2)").setCategory(INPUT_OUTPUT);
    flags.registerRequired('o', OUTPUT_FLAG, File.class, "FILE", "output filename prefix. Use '-' to write to standard output").setCategory(INPUT_OUTPUT);
    CommonFlags.initQualityFormatFlag(flags);
    CommonFlags.initThreadsFlag(flags);
    MapFlags.initAlignerPenaltyFlags(flags);
    flags.registerOptional(MIN_OVERLAP, Integer.class, CommonFlags.INT, "minimum number of bases in overlap for overlap trimming", 25).setCategory(FILTERING);
    flags.registerOptional(MIN_IDENTITY, Integer.class, CommonFlags.INT, "minimum overlap identity required for overlap trimming", 90).setCategory(FILTERING);
    flags.registerOptional(INTERLEAVE, "interleave paired data into a single output file. Default is to split to separate output files").setCategory(UTILITY);
    flags.registerOptional(PROBE_LENGTH, Integer.class, CommonFlags.INT, "assume R1 starts with probes this long, and remove R2 bases that overlap into this", 37).setCategory(FILTERING);
    flags.registerOptional(BATCH_SIZE, Integer.class, CommonFlags.INT, "number of pairs to process per batch", 100000).setCategory(FILTERING);
    CommonFlags.initNoGzip(flags);

    flags.setValidator(new FlagsValidator());
  }

  static class FlagsValidator implements Validator {
    @Override
    public boolean isValid(CFlags flags) {
      final File baseOutput = (File) flags.getValue(OUTPUT_FLAG);
      final boolean gzip = !flags.isSet(NO_GZIP);
      final BaseFile baseFile = FastqUtils.baseFile(baseOutput, gzip);
      if (flags.isSet(INTERLEAVE)) {
        if (!CommonFlags.validateOutputFile(flags, baseFile.suffixedFile(""))) {
          return false;
        }

      } else {
        if (FileUtils.isStdio(baseOutput)) {
          flags.setParseMessage("Sending non-interleaved paired-end data to stdout is not supported.");
          return false;
        }
        if (!(CommonFlags.validateOutputFile(flags, baseFile.suffixedFile("_1")) && CommonFlags.validateOutputFile(flags, baseFile.suffixedFile("_2")))) {
          return false;
        }
      }
      return CommonFlags.validateInputFile(flags, LEFT)
        && CommonFlags.validateInputFile(flags, RIGHT)
        && flags.checkInRange(BATCH_SIZE, 1, Integer.MAX_VALUE)
        && flags.checkInRange(MIN_OVERLAP, 0, Integer.MAX_VALUE)
        && flags.checkInRange(MIN_IDENTITY, 100, Integer.MAX_VALUE)
        && flags.checkInRange(PROBE_LENGTH, 0, Integer.MAX_VALUE);
    }
  }

  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {
    final boolean gzip = !mFlags.isSet(NO_GZIP);
    final File baseOutput = (File) mFlags.getValue(OUTPUT_FLAG);
    final boolean interleavePaired = mFlags.isSet(INTERLEAVE);
    final int batchSize = (Integer) mFlags.getValue(BATCH_SIZE);
    final PairAlignmentStats stats = new PairAlignmentStats();
    final FastqSequenceDataSource.FastQScoreType encoding = FastqTrim.qualityFlagToFastQScoreType((String) mFlags.getValue(QUALITY_FLAG));
    // All trimming and aligning is done in separate threads from reading
    final int threads = CommonFlags.parseThreads((Integer) mFlags.getValue(CommonFlags.THREADS_FLAG));
    try (final SequenceDataSource r1fq = new FastqSequenceDataSource(Collections.singletonList(FileUtils.createInputStream((File) mFlags.getValue(LEFT), true)), encoding);
         final SequenceDataSource r2fq = new FastqSequenceDataSource(Collections.singletonList(FileUtils.createInputStream((File) mFlags.getValue(RIGHT), true)), encoding)) {

      final Timer t = new Timer("FastqPairTrimmer");
      t.start();

      final BaseFile baseFile = FastqUtils.baseFile(baseOutput, gzip);
      final FastqWriter left;
      final FastqWriter right;
      if (interleavePaired) {
        left = new FastqWriter(new OutputStreamWriter(FileUtils.createOutputStream(baseFile, "")));
        right = left;
      } else {
        if (FileUtils.isStdio(baseOutput)) {
          throw new NoTalkbackSlimException("Sending non-interleaved paired-end data to stdout is not supported.");
        }
        left = new FastqWriter(new OutputStreamWriter(FileUtils.createOutputStream(baseFile, "_1")));
        right = new FastqWriter(new OutputStreamWriter(FileUtils.createOutputStream(baseFile, "_2")));
      }
      try (final AsyncFastqPairWriter w = new AsyncFastqPairWriter(left, right)) {
        final BatchReorderingWriter<FastqPair> batchWriter = new BatchReorderingWriter<>(w);
        final Function<Batch<FastqPair>, Runnable> listRunnableFunction = batch -> new PairAlignmentProcessor(stats, batchWriter, batch, getPairAligner());
        final BatchProcessor<FastqPair> fastqPairBatchProcessor = new BatchProcessor<>(listRunnableFunction, threads, batchSize);
        fastqPairBatchProcessor.process(new FastqPairIterator(new FastqIterator(r1fq), new FastqIterator(r2fq)));
      }
      t.stop(stats.mTotal);
      t.log();
      stats.printSummary();
    }
    return 0;
  }

  private PairAligner getPairAligner() {
    final int maxReadLength = 300;
    final int seedLength = 5;
    final NgsParams ngsParams = MapParamsHelper.populateAlignerPenaltiesParams(NgsParams.builder(), mFlags).singleIndelPenalties(null).create();
    return new PairAligner(
      new UnidirectionalAdaptor(new SingleIndelSeededEditDistance(ngsParams, false, seedLength, 2, 2, maxReadLength)),
      (Integer) mFlags.getValue(MIN_OVERLAP), (Integer) mFlags.getValue(MIN_IDENTITY), (Integer) mFlags.getValue(PROBE_LENGTH));
  }

  @Override
  public String moduleName() {
    return PETRIM_MODULE_NAME;
  }

  @Override
  public String description() {
    return "aligns read arms against each other and trims based on the alignment";
  }
}