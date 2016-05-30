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
package com.rtg.variant.bayes.snp;


import com.rtg.launcher.GlobalFlags;
import com.rtg.mode.DNA;
import com.rtg.reader.CgUtils;
import com.rtg.sam.BadSuperCigarException;
import com.rtg.sam.SamUtils;
import com.rtg.sam.SuperCigarParser;
import com.rtg.util.machine.MachineType;
import com.rtg.variant.ReadParserInterface;
import com.rtg.variant.VariantAlignmentRecord;
import com.rtg.variant.VariantParams;
import com.rtg.variant.util.VariantUtils;

/**
 * Generate matcher reports from a CIGAR.
 */
public class CigarParserModel implements ReadParserInterface {

  private static final boolean ILLUMINA_HOMOPOLYMER_HACK = false; //Boolean.valueOf(System.getProperty("com.rtg.variant.illuhomopoly", "false"));

  private static final boolean SOFT_CLIP_COMPLEX_TRIGGER = GlobalFlags.getBooleanValue(GlobalFlags.SOFT_CLIP_COMPLEX_TRIGGER);

  static int getDNA(final char charAt) {
    return DNA.valueOf(charAt).ordinal();
  }

  /** Handles explicit nucleotides and deletions. */
  private final MatcherInterface mMatcherNt;

  /** Handles inserts. */
  private final MatcherInterface mMatcherIndel;

  private final int mStart;

  private final int mEnd;

  private final ToMatcherCigarParser mParser = new ToMatcherCigarParser();

  private final VariantParams mParams;

  /**
   * Construct a new formatter using the specified matchers.
   * @param matcherNt matcher for nucleotides and deletes.
   * @param indelMatcher matcher for insertions (can be null).
   * @param start of region to be processed (0 based, inclusive)
   * @param end of region to be processed (0 based, exclusive)
   * @param params command line parameters.
   */
  public CigarParserModel(final MatcherInterface matcherNt, MatcherInterface indelMatcher, int start, int end, VariantParams params) {
    mMatcherNt = matcherNt;
    mMatcherIndel = indelMatcher;
    mStart = start;
    mEnd = end;
    mParams = params;
//    System.err.println("iontorr? " + mParams.ionTorrent());
  }

  int getReadScore(VariantAlignmentRecord var) {
    return VariantUtils.readScoreFromAlignmentRecord(var, mParams);
  }

  @Override
  public void toMatcher(VariantAlignmentRecord var, MachineType machineType, int qdefault, byte[] templateBytes) throws BadSuperCigarException {
    final byte[] qualities;
    try {
      // Note that we don't use the SuperCigar for CG here, as the overlaps would be treated as double-evidence (whereas allpaths does the right thing).
      mParser.setStandardCigar(var.getCigar(), DNA.byteDNAtoByte(var.getRead()), var.getRead().length);
    } catch (final IllegalArgumentException iae) {
      throw new BadSuperCigarException("Illegal DNA character: " + iae.getMessage());
    }
    qualities = var.getRecalibratedQuality().length == 0 || mParams.ignoreQualityScores() ? null : var.getRecalibratedQuality();

    mParser.setTemplateStart(var.getStart());
    mParser.setTemplate(templateBytes);
    mParser.setQualities(qualities);
    mParser.setAdditional(machineType, getReadScore(var), qdefault, var.isFirst(), !var.isNegativeStrand(), var.isReadPaired(), var.isMated(), var.isUnmapped());

    mParser.parse();
  }

  class ToMatcherCigarParser extends SuperCigarParser {

    ToMatcherCigarParser() { }

    private boolean mIsForward;
    private boolean mIsReadPaired;
    private boolean mIsMated;
    private byte[] mQualities = null;
    private boolean mCgTrimOuterBases;
    private boolean mIsCgOverlapLeft = false;
    private int mMapScore;
    private int mDefaultQuality;
    private boolean mInBlock;

    @Override
    public void setCigar(String superCigar, String readDelta) {
      super.setCigar(superCigar, readDelta);
      mQualities = null;
    }

    @Override
    public void setStandardCigar(String cigar, byte[] read, int readLength) {
      super.setStandardCigar(cigar, read, readLength);
      mQualities = null;
    }

    /**
     * Set the expanded qualities for this SAM record
     * @param qualities the expanded qualities
     */
    void setQualities(byte[] qualities) {
      mQualities = qualities;
    }

    int getCurrentQuality() throws BadSuperCigarException {
      if (mQualities != null && getReadPosition() >= mQualities.length) {
        throw new BadSuperCigarException("readPos " + getReadPosition() + " > qual.len in SAM record");
      }
      return mQualities == null ? mDefaultQuality : mQualities[getReadPosition()];
    }

    void setAdditional(MachineType machineType, int mapScore, int defaultQuality, boolean isFirst, boolean isForward, boolean isReadPaired, boolean isMated, boolean isUnmapped) {
      mCgTrimOuterBases = machineType != null && machineType.cgTrimOuterBases();
      mMapScore = mapScore;
      mDefaultQuality = defaultQuality;
      mIsCgOverlapLeft = machineType == MachineType.COMPLETE_GENOMICS && (isFirst ^ !isForward)
        || machineType == MachineType.COMPLETE_GENOMICS_2 && isForward;
      mIsForward = isForward;
      mIsReadPaired = isReadPaired;
      mIsMated = isMated;
      mIsUnmapped = isUnmapped;
    }

    @Override
    protected void doChunk(char ch, int count) throws BadSuperCigarException {
      mInBlock = false;
      if ((ch == SamUtils.CIGAR_SAME || ch == SamUtils.CIGAR_MISMATCH || ch == SamUtils.CIGAR_SAME_OR_MISMATCH || ch == SamUtils.CIGAR_DELETION_FROM_REF)
          && getTemplatePosition() + count > getTemplateLength()) {
        throw new BadSuperCigarException("Cigar exceeds template length");
      }
      super.doChunk(ch, count);
    }

    @Override
    public void parse() throws BadSuperCigarException {
      super.parse();
    }

    @Override
    protected void doReadOnly(int readNt) {
      if (mInBlock) {
        return;
      }
      //System.err.println("i=" + readNt);
      mInBlock = true;
      if (mMatcherIndel != null && includeBase(getReadPosition()) && mStart <= getTemplatePosition() && getTemplatePosition() < mEnd) {
        mMatcherIndel.match(getTemplatePosition(), EvidenceIndelFactory.SINGLETON.evidence(EvidenceIndel.INSERT, /*unused*/0, /*unused*/0, mMapScore, /*unused*/0, /*unused*/0, mOperationLength, false));
      }
    }

    @Override
    protected void doReadSoftClip(int readNt) {
      if (SOFT_CLIP_COMPLEX_TRIGGER) {
        if (mInBlock) {
          return;
        }
        //System.err.println("i=" + readNt);
        mInBlock = true;
        if (mMatcherIndel != null && includeBase(getReadPosition()) && mStart <= getTemplatePosition() && getTemplatePosition() < mEnd) {
          mMatcherIndel.match(getTemplatePosition(), EvidenceIndelFactory.SINGLETON.evidence(getReadPosition() == 0 ? EvidenceIndel.SOFT_CLIP_LEFT : EvidenceIndel.SOFT_CLIP_RIGHT, /*unused*/0, /*unused*/0, mMapScore, /*unused*/0, /*unused*/0, mOperationLength, false));
        }
      }
    }

    @Override
    protected void doTemplateOnly(int templateNt) {
      //System.err.println("d=" + templateNt);
      if (mMatcherIndel != null && includeBase(getReadPosition())) {
        if (mStart <= getTemplatePosition() && getTemplatePosition() < mEnd) {
          mMatcherIndel.match(getTemplatePosition(), EvidenceIndelFactory.SINGLETON.evidence(EvidenceIndel.DELETE, /*unused*/0, /*unused*/0, mMapScore, /*unused*/0, /*unused*/0, mOperationLength, false));
        }
      }
    }

    @Override
    protected void doSubstitution(int readNt, int templateNt) throws BadSuperCigarException {
      //System.err.println("sub: " + readNt + " vs " + templateNt + " rPos=" + getReadPosition() + " tPos=" + getTemplatePosition());
      assert readNt != templateNt : mCigar + "@rpos=" + getReadPosition() + " @tstartpos=" + getTemplateStartPosition() + " : r" + readNt + " == t" + templateNt;
      doSubstitutionOrEquality(readNt);
    }

    @Override
    protected void doEquality(int readNt, int nt) throws BadSuperCigarException {
//      System.err.println("eq: " + DnaUtils.getBase(readNt) + " vs " + DnaUtils.getBase(nt) + " rPos=" + getReadPosition() + " tPos=" + getTemplatePosition());
      assert readNt == nt || nt == DNA.N.ordinal() || readNt == DNA.N.ordinal() : mCigar + "@pos=" + getReadPosition() + " @tstartpos=" + getTemplateStartPosition() + " : r" + readNt + " != t" + nt;
      doSubstitutionOrEquality(readNt);
    }

    private void doSubstitutionOrEquality(final int readNt) throws BadSuperCigarException {
      final boolean should = includeBase(getReadPosition());
      if (should && mStart <= getTemplatePosition() && getTemplatePosition() < mEnd) {
        int currentQuality = getCurrentQuality();
        if (ILLUMINA_HOMOPOLYMER_HACK) {
          final int templatePos = getTemplatePosition();
          if (templatePos > 2 && templatePos < getTemplateLength() - 2) {
            final boolean homop;
            final byte currentTemplate = templateNt(templatePos);
            final boolean match = readNt == currentTemplate || readNt == DNA.N.ordinal() || currentTemplate == DNA.N.ordinal();
            final byte homopTemplate;
            if (mIsForward) {
              //equal if current or previous nt are same or previous and one before are same
              homop = templateNt(templatePos - 1) == templateNt(templatePos - 2);
              homopTemplate = templateNt(templatePos - 1);
            } else {
              homop = templateNt(templatePos + 1) == templateNt(templatePos + 2);
              homopTemplate = templateNt(templatePos + 1);
            }
            if (!match && homop && readNt == homopTemplate) {
              currentQuality = 2;
            }
          }
        }
        mMatcherNt.match(getTemplatePosition(), getReadPosition(), getReadLength() - getReadPosition() - 1, readNt, mMapScore, currentQuality, mMatcherNt.getStateIndex(mIsForward, mIsReadPaired, mIsMated));
        if (mMatcherIndel != null) { //for coverage-sensitive indel triggering, we need to use a dummy match for the indels to work out the coverage down deeper.
          mMatcherIndel.match(getTemplatePosition(), null);
        }
      }
    }

    @Override
    protected void doUnmapped() {
      if (mStart <= getTemplatePosition() && getTemplatePosition() < mEnd) {
        mMatcherNt.unmapped(getTemplatePosition());
      }
    }

    @Override
    protected void doTemplateOverlap() {
    }

    @Override
    protected void doUnknownOnTemplate(int readNt, int templateNt) throws BadSuperCigarException {
      doSubstitutionOrEquality(readNt);
    }

    @Override
    protected void doUnknownOnRead() throws BadSuperCigarException {
      doSubstitutionOrEquality((byte) DNA.N.ordinal());
    }

    /**
     * Determine whether a base should be included, allowing the ability to trim the overlap end of CG version 1 reads
     * @param rPos read position
     * @return true if this base should be included
     */
    private boolean includeBase(int rPos) {
      return !mCgTrimOuterBases
        || (!mIsCgOverlapLeft && rPos < (CgUtils.CG_RAW_READ_LENGTH - CgUtils.CG_OVERLAP_POSITION))
        || (mIsCgOverlapLeft && rPos >= (getReadLength() == 0 ? CgUtils.CG_OVERLAP_POSITION : getReadLength() - (CgUtils.CG_RAW_READ_LENGTH - CgUtils.CG_OVERLAP_POSITION)));
    }

  }
}
