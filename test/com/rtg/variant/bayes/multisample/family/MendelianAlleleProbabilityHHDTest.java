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
import com.rtg.variant.bayes.CodeDiploid;

import junit.framework.TestCase;

/**
 */
public class MendelianAlleleProbabilityHHDTest extends TestCase {

  public void test() {
    final Code code = new CodeDiploid(4);
    assertEquals(0.0, MendelianAlleleProbabilityHHD.SINGLETON_HHD.probabilityLn(code, 0, 0, 0));
    assertEquals(Double.NEGATIVE_INFINITY, MendelianAlleleProbabilityHHD.SINGLETON_HHD.probabilityLn(code, 0, 0, 1));

    assertEquals(0.0, MendelianAlleleProbabilityHHD.SINGLETON_HHD.probabilityLn(code, 0, 1, code.code(0, 1)));
    assertEquals(0.0, MendelianAlleleProbabilityHHD.SINGLETON_HHD.probabilityLn(code, 1, 0, code.code(0, 1)));
    assertEquals(Double.NEGATIVE_INFINITY, MendelianAlleleProbabilityHHD.SINGLETON_HHD.probabilityLn(code, 1, 0, code.code(0, 2)));
  }
}
