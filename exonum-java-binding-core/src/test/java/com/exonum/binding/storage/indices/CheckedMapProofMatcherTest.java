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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.proofs.map.flat.CheckedFlatMapProof;
import com.exonum.binding.common.proofs.map.flat.CheckedMapProof;
import com.exonum.binding.common.proofs.map.flat.MapEntry;
import com.exonum.binding.common.proofs.map.flat.ProofStatus;
import com.exonum.binding.common.serialization.StandardSerializers;
import java.util.Collections;
import org.hamcrest.Description;
import org.hamcrest.StringDescription;
import org.junit.Test;

public class CheckedMapProofMatcherTest {

  @Test
  public void matchesInvalidProof() {
    HashCode key = HashCode.fromString("ab");
    CheckedMapProofMatcher matcher = CheckedMapProofMatcher.isValid(key, null);

    CheckedMapProof proof = CheckedFlatMapProof.invalid(
        ProofStatus.DUPLICATE_PATH);

    assertFalse(matcher.matchesSafely(proof));
  }

  @Test
  public void matchesValidProof() {
    HashCode key = HashCode.fromString("ab");
    String value = "hello";

    CheckedMapProofMatcher matcher = CheckedMapProofMatcher.isValid(key, value);

    MapEntry entry = new MapEntry(key.asBytes(), StandardSerializers.string().toBytes(value));

    HashCode rootHash = HashCode.fromString("123456ef");
    CheckedMapProof proof = CheckedFlatMapProof.correct(
        rootHash,
        Collections.singletonList(entry),
        Collections.emptyList());

    assertThat(proof, matcher);
  }

  @Test
  public void describeMismatchSafelyCorrectProof() {
    HashCode key = HashCode.fromString("ab");
    String expectedValue = null;  // No value
    CheckedMapProofMatcher matcher = CheckedMapProofMatcher.isValid(key, expectedValue);

    String actualValue = "hello";
    MapEntry entry = new MapEntry(key.asBytes(), StandardSerializers.string().toBytes(actualValue));
    HashCode rootHash = HashCode.fromString("123456ef");
    CheckedMapProof proof = CheckedFlatMapProof.correct(
        rootHash,
        Collections.singletonList(entry),
        Collections.emptyList());

    Description d = new StringDescription();
    matcher.describeMismatchSafely(proof, d);

    assertThat(d.toString(), equalTo("was a valid proof, entries=[(ab -> hello)], "
        + "missing keys=[], Merkle root=<123456ef>"));
  }

  @Test
  public void describeMismatchSafelyInvalidProof() {
    HashCode key = HashCode.fromString("ab");
    CheckedMapProofMatcher matcher = CheckedMapProofMatcher.isValid(key, null);

    CheckedMapProof proof = CheckedFlatMapProof.invalid(
        ProofStatus.DUPLICATE_PATH);


    Description d = new StringDescription();
    matcher.describeMismatchSafely(proof, d);
    assertThat(d.toString(), equalTo("was an invalid proof, status=<"
        + ProofStatus.DUPLICATE_PATH + ">"));
  }
}
