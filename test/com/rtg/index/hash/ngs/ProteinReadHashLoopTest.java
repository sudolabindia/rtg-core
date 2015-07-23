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
package com.rtg.index.hash.ngs;

import java.io.IOException;
import java.io.StringWriter;

import com.rtg.index.hash.ngs.general.Skeleton;
import com.rtg.index.hash.ngs.instances.AbstractSplitTest.ReadCallMock;

import junit.framework.TestCase;

/**
 * Test for corresponding class
 */
public class ProteinReadHashLoopTest extends TestCase {

  public void testHashCall() throws IOException {
    final FakeProteinMask mask = new FakeProteinMask(new Skeleton(12, 12, 0, 0, 1), new ReadCallMock(new StringWriter()), new ImplementHashFunctionTest.TemplateCallMock());
    final ProteinReadHashLoop loop = new ProteinReadHashLoop(12, 12, mask);
    assertEquals(0, mask.mReadCalls);
    loop.hashCall(0, 0);
    assertEquals(1, mask.mReadCalls);
    try {
      loop.hashCallBidirectional(0, 0, 0, 0);
    } catch (UnsupportedOperationException e) {
      assertEquals("Not supported.", e.getMessage());
    }
  }

}
