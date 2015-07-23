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

package com.rtg.report;

import com.rtg.ngs.NgsFilterParams;
import com.rtg.ngs.NgsOutputParams;
import com.rtg.ngs.NgsParams;

import junit.framework.TestCase;

/**
 */
public class MapXSummaryReportTest extends TestCase {
  public void testMapxSummary() {
    final MapXSummaryReport summary = new MapXSummaryReport();
    summary.setParams(NgsParams.builder().outputParams(NgsOutputParams.builder().filterParams(NgsFilterParams.builder().maxTopResults(42).create()).create()).create());
    final StringBuilder sb = new StringBuilder();
    summary.reportingReport(sb);
    assertTrue(sb.toString(), sb.toString().contains("Top 42 positions."));

  }
}
