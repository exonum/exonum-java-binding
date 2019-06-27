/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.core.storage.indices;

import static com.exonum.binding.core.storage.indices.MapTestEntry.absentEntry;
import static com.exonum.binding.core.storage.indices.MapTestEntry.presentEntry;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.exonum.binding.common.collect.MapEntry;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.proofs.map.CheckedFlatMapProof;
import com.exonum.binding.common.proofs.map.CheckedMapProof;
import com.exonum.binding.common.proofs.map.MapProofStatus;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hamcrest.Description;
import org.hamcrest.StringDescription;
import org.junit.jupiter.api.Test;

class CheckedMapProofMatcherTest {

  private static final HashCode TEST_KEY1 = HashCode.fromString("ab");
  private static final HashCode TEST_KEY2 = HashCode.fromString("cd");
  private static final String TEST_VALUE = "hello";
  private static final List<MapTestEntry> TEST_ENTRY_LIST = Arrays
      .asList(presentEntry(TEST_KEY1, TEST_VALUE), absentEntry(TEST_KEY2));
  private static final HashCode ROOT_HASH = HashCode.fromString("123456ef");

  @Test
  void matchesInvalidProof() {
    CheckedMapProofMatcher matcher =
        CheckedMapProofMatcher.isValid(TEST_ENTRY_LIST);

    CheckedMapProof proof = CheckedFlatMapProof.invalid(
        MapProofStatus.DUPLICATE_PATH);

    assertFalse(matcher.matchesSafely(proof));
  }

  @Test
  void matchesValidProof() {
    CheckedMapProofMatcher matcher = CheckedMapProofMatcher.isValid(TEST_ENTRY_LIST);

    MapEntry<ByteString, ByteString> entry =
        MapEntry.valueOf(toByteString(TEST_KEY1), ByteString.copyFromUtf8(TEST_VALUE));

    CheckedMapProof proof = CheckedFlatMapProof.correct(
        ROOT_HASH,
        Collections.singleton(entry),
        Collections.singleton(toByteString(TEST_KEY2)));

    assertThat(proof, matcher);
  }

  @Test
  void describeMismatchSafelyCorrectProof() {
    HashCode presentKey = HashCode.fromString("ab");
    HashCode absentKey = HashCode.fromString("cd");
    String expectedValue = "different value";
    List<MapTestEntry> expectedEntryList =
        Arrays.asList(presentEntry(presentKey, expectedValue), absentEntry(absentKey));
    CheckedMapProofMatcher matcher = CheckedMapProofMatcher.isValid(expectedEntryList);

    ByteString actualValue = ByteString.copyFromUtf8("hello");
    MapEntry<ByteString, ByteString> entry = MapEntry.valueOf(toByteString(presentKey),
        actualValue);
    HashCode rootHash = HashCode.fromString("123456ef");
    CheckedMapProof proof = CheckedFlatMapProof.correct(
        rootHash,
        Collections.singleton(entry),
        Collections.singleton(toByteString(absentKey)));

    Description d = new StringDescription();
    matcher.describeMismatchSafely(proof, d);

    assertThat(d.toString(), equalTo("was a valid proof, entries=[(ab -> hello)], "
        + "missing keys=[cd], Merkle root=<123456ef>"));
  }

  @Test
  void describeMismatchSafelyInvalidProof() {
    CheckedMapProofMatcher matcher = CheckedMapProofMatcher.isValid(TEST_ENTRY_LIST);

    CheckedMapProof proof = CheckedFlatMapProof.invalid(
        MapProofStatus.DUPLICATE_PATH);

    Description d = new StringDescription();
    matcher.describeMismatchSafely(proof, d);
    assertThat(d.toString(), equalTo("was an invalid proof, status=<"
        + MapProofStatus.DUPLICATE_PATH + ">"));
  }

  private static ByteString toByteString(HashCode hashCode) {
    return ByteString.copyFrom(hashCode.asBytes());
  }
}
