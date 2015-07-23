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
package com.rtg.util.memory;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test class for IdentitySet. <p>
 *
 * Run from the command line with:<p>
 *
 * <code>
 * java junit.textui.TestRunner com.rtg.util.memory.IdentitySetTest<br>
 * java junit.swingui.TestRunner com.rtg.util.memory.IdentitySetTest<br>
 * java com.rtg.util.memory.IdentitySetTest<br>
 * </code>
 *
 */
public class IdentitySetTest extends TestCase {

  public IdentitySetTest(final String s) {
    super(s);
  }


  public void testDefault() {
    final IdentitySet set = new IdentitySet();
    tst(set);
  }


  public void testMin() {
    final IdentitySet set = new IdentitySet(0);
    tst(set);
    //System.err.println(set.dumpString());
  }


  public void testBig() {
    final IdentitySet set = new IdentitySet(10000);
    tst(set);
  }

  public void testBigger() {
    final IdentitySet set = new IdentitySet(5000);
    final Integer[] tint = new Integer[5000];
    for (int i = 0; i < tint.length; i++) {
      tint[i] = 1000;
    }
    for (Integer aTint1 : tint) {
      set.add(aTint1);
    }
    for (Integer aTint : tint) {
      final boolean cond = set.contains(aTint);
      assertTrue(cond);
    }
  }

  private static void tst(final IdentitySet set) {
    final Integer[] tint = new Integer[1000];
    assertTrue(set.isEmpty());
    assertEquals(0, set.size());
    for (int i = 0; i < tint.length; i++) {
      final Integer xint = 10000;
      tint[i] = xint;
      assertTrue(!set.contains(xint));
      assertTrue(set.add(xint));
      assertTrue(!set.add(xint));
      assertTrue(set.contains(xint));
      assertTrue(!set.isEmpty());
      assertEquals(i + 1, set.size());
      set.integrity();
      for (int j = 0; j <= i; j++) {
        assertTrue(set.contains(tint[j]));
      }
    }
    for (Integer aTint : tint) {
      assertTrue(set.contains(aTint));
    }
    final Integer[] tint2 = new Integer[5];
    for (int i = 0; i < tint2.length - 1; i++) {
      tint2[i] = tint.length + i;
    }
    set.addAll(tint2);
    assertEquals(tint.length + tint2.length - 1, set.size());
    for (int i = 0; i < tint2.length - 1; i++) {
      assertTrue(set.contains(tint2[i]));
    }
  }


  /**
   * Adds tests to suite to be run by main
   *
   * @return The test suite.
   */
  public static Test suite() {
    return new TestSuite(IdentitySetTest.class);
    //suite.addTest(new IdentitySetTest("testIdentitySet"));
  }


  /**
   * Main method needed to.create a self runnable class
   *
   * @param args The command line arguments.
   */
  /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }

}


