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
import com.exonum.binding.common.proofs.map.flat.CheckedMapProof;
import com.exonum.binding.common.proofs.map.flat.MapEntry;
import com.exonum.binding.common.proofs.map.flat.ProofStatus;
import java.util.List;
import javax.annotation.Nullable;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;

class CheckedMapProofMatcher extends TypeSafeMatcher<CheckedMapProof> {

  private final HashCode key;
  @Nullable
  private final String expectedValue;

  private final Matcher<byte[]> keyMatcher;
  private final Matcher<byte[]> valueMatcher;

  private CheckedMapProofMatcher(
      HashCode key, @Nullable String expectedValue) {
    this.key = checkNotNull(key);
    this.expectedValue = expectedValue;
    keyMatcher = IsEqual.equalTo(key.asBytes());
    valueMatcher =
        expectedValue != null ? IsEqual.equalTo(expectedValue.getBytes()) : IsEqual.equalTo(null);
  }

  @Override
  protected boolean matchesSafely(CheckedMapProof checkedMapProof) {
    ProofStatus status = checkedMapProof.getStatus();
    List<byte[]> missingKeys = checkedMapProof.getMissingKeys();
    List<MapEntry> entries = checkedMapProof.getEntries();

    // In case of null expectedValue the absence of the key is checked
    if (expectedValue == null) {
      return status == ProofStatus.CORRECT
          && missingKeys.size() == 1
          && entries.isEmpty()
          && keyMatcher.matches(missingKeys.get(0));
    } else {
      return status == ProofStatus.CORRECT
          && entries.size() == 1
          && missingKeys.isEmpty()
          && keyMatcher.matches(entries.get(0).getKey())
          && valueMatcher.matches(entries.get(0).getValue());
    }
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("valid proof, key=").appendText(key.toString())
        .appendText(", value=").appendText(expectedValue);
  }

  /**
   * Creates a matcher of a checked proof that is valid, has the same key and value as specified
   * and has the expected root hash.
   *
   * @param key a requested key
   * @param expectedValue a value that is expected to be mapped to the requested key, or null if
   *     there must not be such mapping in the proof map
   */
  static CheckedMapProofMatcher isValid(HashCode key, @Nullable String expectedValue) {
    return new CheckedMapProofMatcher(key, expectedValue);
  }
}
