/*
 * Copyright (c) 2014. Real Time Genomics Limited.
 *
 * Use of this source code is bound by the Real Time Genomics Limited Software Licence Agreement
 * for Academic Non-commercial Research Purposes only.
 *
 * If you did not receive a license accompanying this file, a copy must first be obtained by email
 * from support@realtimegenomics.com.  On downloading, using and/or continuing to use this source
 * code you accept the terms of that license agreement and any amendments to those terms that may
 * be made from time to time by Real Time Genomics Limited.
 */
package com.rtg.sam.probe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.rtg.util.intervals.RangeList;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

/**
 *
 */
class PosChecker extends PositionAndStrandChecker {
  PosChecker(int tolerance) {
    super(tolerance);
  }

  @Override
  public boolean check(SAMRecord record, RangeList.RangeData<String> data) {
    final int alignmentStart = record.getAlignmentStart() - 1;
    if (!record.getReadNegativeStrandFlag()) {
      if (data.getStart() > alignmentStart - mTolerance && data.getStart() < alignmentStart + mTolerance) {
//                    System.err.println(record.getSAMString() + " strip forward to: " + data.getEnd() + " (" + data.getStart() + " : " + data.getEnd() + ")");
        return true;
      }
    }
    return false;
  }

  @Override
  public int getStartDataIndex(SAMRecord record, RangeList<String> list) {
    final int alignmentStart = record.getAlignmentStart() - 1;
    return list.findFullRangeIndex(alignmentStart - mTolerance);
  }

  @Override
  void stripRecord(SAMRecord record, RangeList.RangeData<String> data) {
    final int diff = record.getAlignmentStart() - 1 - data.getStart();
    mPosDiffStats[mTolerance + diff]++;
    setAlignmentStart(record, data.getEnd());
  }

  void setAlignmentStart(SAMRecord record, int alignmentStart) {
    int readStart = 0;
    int refStart = record.getAlignmentStart() - 1;
    final List<CigarElement> cigarElements = new ArrayList<>();
    for (CigarElement e : record.getCigar().getCigarElements()) {
      final CigarOperator operator = e.getOperator();
      if (alignmentStart > refStart) {
        final int consume = operator.consumesReferenceBases() ? Math.min(alignmentStart - refStart, e.getLength()) : e.getLength();
        if (operator.consumesReferenceBases()) {
          refStart += consume;
        }
        if (operator.consumesReadBases()) {
          readStart += consume;
        }
        updateStrippedStats(operator, consume);
        if (e.getLength() - consume > 0) {
          cigarElements.add(new CigarElement(e.getLength() - consume, operator));
        }
      } else {
        cigarElements.add(e);
      }
    }
    final byte[] readBases = record.getReadBases();
    record.setReadBases(Arrays.copyOfRange(readBases, readStart, readBases.length));
    final byte[] baseQualities = record.getBaseQualities();
    record.setBaseQualities(Arrays.copyOfRange(baseQualities, readStart, readBases.length));
    record.setCigar(new Cigar(cigarElements));
    record.setAlignmentStart(alignmentStart + 1);
  }

}