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
package com.rtg.ml;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.StringUtils;
import com.rtg.util.Utils;

/**
 * An ensemble classifier.
 *  Implements the predictor side of the ensemble
 *
 */
@TestClass("com.rtg.ml.BaggedClassifierBuilderTest")
final class BaggedClassifier implements PredictClassifier {

  // Allows loading a model with a large number of sub-classifiers and saving a version containing only the first N sub-classifiers
  private static final int CLASSIFIER_SAVE_LIMIT = Integer.parseInt(System.getProperty("bagging.save-limit", Integer.toString(Integer.MAX_VALUE)));

  static final int SERIAL_VERSION = 1;

  private final PredictClassifier[] mClassifiers;

  BaggedClassifier(PredictClassifier... classifiers) {
    mClassifiers = classifiers;
  }

  BaggedClassifier(DataInputStream dis, Dataset data) throws IOException {
    final int version = dis.readInt();
    if (version == 1) {
      final int length = dis.readInt();
      mClassifiers = new PredictClassifier[length];
      for (int i = 0; i < mClassifiers.length; ++i) {
        mClassifiers[i] = MlPredictLoader.loadPredictClassifier(dis, data);
      }
    } else {
      throw new IOException("Unsupported bag version: " + version);
    }
  }

  @Override
  public void save(DataOutputStream dos, Dataset data) throws IOException {
    dos.writeInt(MlPredictLoader.MlPredictType.BAGGED.ordinal()); //tell loader which type to load
    dos.writeInt(SERIAL_VERSION);
    final int length = Math.min(mClassifiers.length, CLASSIFIER_SAVE_LIMIT);
    dos.writeInt(length);
    for (int i = 0; i < length; i++) {
      mClassifiers[i].save(dos, data);
    }
  }

  @Override
  public double predict(double[] instance) {
    double prob = 0;
    for (PredictClassifier classifier : mClassifiers) {
      prob += classifier.predict(instance);
    }
    return prob / mClassifiers.length;
  }

  @Override
  public int hashCode() {
    return Utils.hash(mClassifiers);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof BaggedClassifier)) {
      return false;
    }
    final BaggedClassifier obj1 = (BaggedClassifier) obj;
    return Arrays.equals(mClassifiers, obj1.mClassifiers);
  }

  @Override
  public StringBuilder toString(StringBuilder out, String indent, Dataset data) {
    final String newIndent = indent + "  ";
    for (int i = 0; i < mClassifiers.length; ++i) {
      out.append(indent).append("classifier [").append(i + 1).append("]").append(StringUtils.LS);
      mClassifiers[i].toString(out, newIndent, data);
    }
    return out;
  }
}
