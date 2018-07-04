/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
