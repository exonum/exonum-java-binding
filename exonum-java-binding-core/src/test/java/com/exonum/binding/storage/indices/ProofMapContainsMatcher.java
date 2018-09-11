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
import com.exonum.binding.storage.proofs.map.MapProof;
import com.exonum.binding.storage.proofs.map.MapProofValidator;
import com.exonum.binding.storage.serialization.StandardSerializers;
import javax.annotation.Nullable;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

class ProofMapContainsMatcher extends TypeSafeMatcher<ProofMapIndexProxy<HashCode, String>> {

  private final HashCode key;
  private final MapProofValidatorMatcher proofValidatorMatcher;

  private ProofMapContainsMatcher(HashCode key, @Nullable String expectedValue) {
    this.key = key;
    proofValidatorMatcher = MapProofValidatorMatcher.isValid(key, expectedValue);
  }

  @Override
  protected boolean matchesSafely(ProofMapIndexProxy<HashCode, String> map) {
    MapProofValidator validator = checkProof(map);

    return proofValidatorMatcher.matches(validator);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("proof map providing ")
        .appendDescriptionOf(proofValidatorMatcher);
  }

  @Override
  protected void describeMismatchSafely(ProofMapIndexProxy<HashCode, String> map,
                                        Description mismatchDescription) {
    MapProofValidator validator = checkProof(map);
    proofValidatorMatcher.describeMismatch(validator, mismatchDescription);
  }

  private MapProofValidator checkProof(ProofMapIndexProxy<HashCode, String> map) {
    MapProof proof = map.getProof(key);
    assert proof != null : "The proof must not be null";
    HashCode rootHash = map.getRootHash();
    MapProofValidator<String> validator = new MapProofValidator<>(rootHash, key.asBytes(),
        StandardSerializers.string());

    proof.accept(validator);
    return validator;
  }

  /**
   * Creates a matcher for a proof map that matches iff the map provides a valid proof
   * that it maps the specified value to the specified key.
   *
   * @param key a key to request proof for
   * @param value an expected value mapped to the key
   */
  static ProofMapContainsMatcher provesThatContains(HashCode key, String value) {
    return new ProofMapContainsMatcher(key, checkNotNull(value));
  }

  /**
   * Creates a matcher for a proof map that matches iff the map provides a valid proof
   * that it does not map any value to the specified key.
   *
   * @param key a key to request proof for
   */
  static ProofMapContainsMatcher provesNoMappingFor(HashCode key) {
    return new ProofMapContainsMatcher(key, null);
  }
}
