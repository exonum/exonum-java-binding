package com.exonum.binding.test;

public final class TestParameters {

  /**
   * A syntactic sugar to fluently convert a list of Objects to an array.
   *
   * <p>Instead of: <code>new Object[]{o1, o2, o3}</code>,
   * you get: <code>parameters(o1, o2, o3)</code>.
   *
   * <p>Use in {@link org.junit.runners.Parameterized} tests.
   */
  public static Object[] parameters(Object... testParameters) {
    return testParameters;
  }

  private TestParameters() {}
}
