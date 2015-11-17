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
package com.rtg.index.hash.ngs.instances;

import com.rtg.index.hash.ngs.ReadCall;
import com.rtg.index.hash.ngs.TemplateCall;
import com.rtg.util.diagnostic.Diagnostic;

import junit.framework.TestCase;

/**
 */
public class DummyCGMaskTest extends TestCase {

  private class MyAbstractCGMask extends AbstractCGMask {

    MyAbstractCGMask(final int windowSize, final ReadCall readCall, final TemplateCall templateCall) {
      super(windowSize, readCall, templateCall);
      long mLl = cgAdjust(5);
      assertEquals(5, mLl);

      mLl = cgAdjust(0b11111111111111111111111110000000000000000L
                   | 0b00000000000000000000000000000001111111111L);
      assertEquals(0b11111111111111111111111111111111111L, mLl);
    }

    @Override
    public void readAll(int readId, long v0, long v1) {
      // unused

    }
    @Override
    public void templateAll(int endPosition, long v0, long v1) {
      // unused

    }
  }

  public void testCgAdjust() {
    Diagnostic.setLogStream();
    new MyAbstractCGMask(18, null, null);
  }

}
