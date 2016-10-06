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

package com.rtg.variant.bayes.multisample.family;

import com.rtg.variant.bayes.Code;

/**
 * Situation where a diploid child inherits one allele from each parent.
 */
public final class MendelianAlleleProbabilityHHD extends MendelianAlleleProbabilityNormal {

  /** Haploid parents, diploid child. */
  public static final MendelianAlleleProbability SINGLETON_HHD = new MendelianAlleleProbabilityHHD();

  @Override
  public double probabilityLn(Code code, int father, int mother, int child) {
    assert code.homozygous(father);
    assert code.homozygous(mother);
    final int a = code.a(child);
    final int b = code.bc(child);
    return (father == a && mother == b) || (father == b && mother == a) ? 0.0 : Double.NEGATIVE_INFINITY;
  }
}
