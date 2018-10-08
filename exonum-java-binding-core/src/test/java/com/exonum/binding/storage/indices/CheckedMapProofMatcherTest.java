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

import static com.exonum.binding.storage.indices.MapTestEntry.absentEntry;
import static com.exonum.binding.storage.indices.MapTestEntry.presentEntry;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.proofs.map.flat.CheckedFlatMapProof;
import com.exonum.binding.common.proofs.map.flat.CheckedMapProof;
import com.exonum.binding.common.proofs.map.flat.MapEntry;
import com.exonum.binding.common.proofs.map.flat.ProofStatus;
import com.exonum.binding.common.serialization.StandardSerializers;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hamcrest.Description;
import org.hamcrest.StringDescription;
import org.junit.Test;

public class CheckedMapProofMatcherTest {

  private static final HashCode TEST_KEY1 = HashCode.fromString("ab");
  private static final HashCode TEST_KEY2 = HashCode.fromString("cd");
  private static final String TEST_VALUE = "hello";
  private static final List<MapTestEntry> TEST_ENTRY_LIST = Arrays
      .asList(presentEntry(TEST_KEY1, TEST_VALUE), absentEntry(TEST_KEY2));
  private static final HashCode ROOT_HASH = HashCode.fromString("123456ef");

  @Test
  public void matchesInvalidProof() {
    CheckedMapProofMatcher matcher =
        CheckedMapProofMatcher.isValid(TEST_ENTRY_LIST);

    CheckedMapProof proof = CheckedFlatMapProof.invalid(
        ProofStatus.DUPLICATE_PATH);

    assertFalse(matcher.matchesSafely(proof));
  }

  @Test
  public void matchesValidProof() {
    CheckedMapProofMatcher matcher = CheckedMapProofMatcher.isValid(TEST_ENTRY_LIST);

    MapEntry entry =
        new MapEntry(TEST_KEY1.asBytes(), StandardSerializers.string().toBytes(TEST_VALUE));

    CheckedMapProof proof = CheckedFlatMapProof.correct(
        ROOT_HASH,
        Collections.singletonList(entry),
        Collections.singletonList(TEST_KEY2.asBytes()));

    assertThat(proof, matcher);
  }

  @Test
  public void describeMismatchSafelyCorrectProof() {
    HashCode presentKey = HashCode.fromString("ab");
    HashCode absentKey = HashCode.fromString("cd");
    String expectedValue = "different value";
    List<MapTestEntry> expectedEntryList =
        Arrays.asList(presentEntry(presentKey, expectedValue), absentEntry(absentKey));
    CheckedMapProofMatcher matcher = CheckedMapProofMatcher.isValid(expectedEntryList);

    byte[] actualValue = StandardSerializers.string().toBytes("hello");
    MapEntry entry = new MapEntry(presentKey.asBytes(), actualValue);
    HashCode rootHash = HashCode.fromString("123456ef");
    CheckedMapProof proof = CheckedFlatMapProof.correct(
        rootHash,
        Collections.singletonList(entry),
        Collections.singletonList(absentKey.asBytes()));

    Description d = new StringDescription();
    matcher.describeMismatchSafely(proof, d);

    assertThat(d.toString(), equalTo("was a valid proof, entries=[(ab -> hello)], "
        + "missing keys=[cd], Merkle root=<123456ef>"));
  }

  @Test
  public void describeMismatchSafelyInvalidProof() {
    CheckedMapProofMatcher matcher = CheckedMapProofMatcher.isValid(TEST_ENTRY_LIST);

    CheckedMapProof proof = CheckedFlatMapProof.invalid(
        ProofStatus.DUPLICATE_PATH);


    Description d = new StringDescription();
    matcher.describeMismatchSafely(proof, d);
    assertThat(d.toString(), equalTo("was an invalid proof, status=<"
        + ProofStatus.DUPLICATE_PATH + ">"));
  }
}
