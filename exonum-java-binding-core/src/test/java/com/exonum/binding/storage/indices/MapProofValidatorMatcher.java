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

package com.exonum.binding.storage.indices;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.storage.proofs.map.MapProofValidator;
import javax.annotation.Nullable;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;

// This class is a slightly modified copy of the one in exonum-java-binding-common.
// It is specialized to work with maps with HashCode keys and String values only.
class MapProofValidatorMatcher extends TypeSafeMatcher<MapProofValidator<String>> {

  private final HashCode key;
  @Nullable private final String expectedValue;
  private final Matcher<byte[]> keyMatcher;
  private final Matcher<String> valueMatcher;

  private MapProofValidatorMatcher(HashCode key, @Nullable String expectedValue) {
    this.key = checkNotNull(key);
    this.expectedValue = expectedValue;
    keyMatcher = IsEqual.equalTo(key.asBytes());
    valueMatcher = IsEqual.equalTo(expectedValue);
  }

  @Override
  protected boolean matchesSafely(MapProofValidator<String> validator) {
    return validator.isValid()
        && keyMatcher.matches(validator.getKey())
        && validator.getValue()
            .map(valueMatcher::matches)
            .orElse(expectedValue == null);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("valid proof, key=").appendText(key.toString())
        .appendText(", value=").appendText(expectedValue);
  }

  /**
   * Creates a matcher of a proof validator that is valid and has
   * the same key and value as specified.
   *
   * @param key a requested key
   * @param expectedValue a value that is expected to be mapped to the requested key,
   *                      or null if there must not be such mapping in the proof map
   */
  static MapProofValidatorMatcher isValid(HashCode key, @Nullable String expectedValue) {
    return new MapProofValidatorMatcher(key, expectedValue);
  }
}
