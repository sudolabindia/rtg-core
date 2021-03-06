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

package com.rtg.index;

import com.rtg.AbstractTest;

/**
 * Test
 */
public class FixedRepeatFrequencyFilterMethodTest extends AbstractTest {

  public void testSimple() {
    final IndexFilterMethod m = new FixedRepeatFrequencyFilterMethod(50);
    m.initialize(null);
    assertTrue(m.keepHash(0, 5));
    assertTrue(m.keepHash(0, 25));
    assertTrue(m.keepHash(0, 50));
    assertFalse(m.keepHash(0, 80));
    assertFalse(m.keepHash(0, 1000));
    assertFalse(m.keepHash(0, 10000));
  }
}