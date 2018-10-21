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

package com.exonum.binding.common.proofs.map;

import static com.exonum.binding.common.hash.Funnels.hashCodeFunnel;
import static com.exonum.binding.common.proofs.DbKeyFunnel.dbKeyFunnel;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.HashFunction;
import com.exonum.binding.common.hash.Hashing;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UncheckedFlatMapProofTest {

  private static final ByteString FIRST_VALUE = ByteString.copyFromUtf8("testValue");
  private static final ByteString SECOND_VALUE = ByteString.copyFromUtf8("anotherTestValue");
  private static final ByteString THIRD_VALUE = ByteString.copyFromUtf8("oneMoreTestValue");

  private static final HashFunction HASH_FUNCTION = Hashing.defaultHashFunction();

  @Test
  void mapProofShouldBeCorrect() {
    DbKey firstDbKey = DbKeyTestUtils.branchKeyFromPrefix("101100");
    ByteString valueKey = DbKeyTestUtils.keyByteStringFromString("101110");
    DbKey thirdDbKey = DbKeyTestUtils.branchKeyFromPrefix("1011111");

    MapEntry leaf = createMapEntry(valueKey, FIRST_VALUE);
    List<MapProofEntry> branches = Arrays.asList(
        createMapProofEntry(firstDbKey),
        createMapProofEntry(thirdDbKey)
    );
    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(
            branches, singletonList(leaf), emptyList());

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();

    MapEntry expectedEntry = new MapEntry(valueKey, FIRST_VALUE);
    assertThat(checkedMapProof.getEntries(), equalTo(singleton(expectedEntry)));
    assertTrue(checkedMapProof.containsKey(valueKey));
    assertThat(checkedMapProof.get(valueKey), equalTo(FIRST_VALUE));
  }

  @Test
  void mapProofWithSeveralLeafsShouldBeCorrect() {
    ByteString firstKey = DbKeyTestUtils.keyByteStringFromString("0011_0101");
    ByteString secondKey = DbKeyTestUtils.keyByteStringFromString("0011_0110");
    DbKey thirdDbKey = DbKeyTestUtils.branchKeyFromPrefix("0100_0000");
    ByteString fourthKey = DbKeyTestUtils.keyByteStringFromString("1000_1101");

    List<MapEntry> leaves = Arrays.asList(
        createMapEntry(firstKey, FIRST_VALUE),
        createMapEntry(secondKey, SECOND_VALUE),
        createMapEntry(fourthKey, THIRD_VALUE)
    );

    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(
            singletonList(createMapProofEntry(thirdDbKey)),
            leaves,
            emptyList());

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();

    List<MapEntry> expectedEntriesList = Arrays.asList(
        new MapEntry(firstKey, FIRST_VALUE),
        new MapEntry(secondKey, SECOND_VALUE),
        new MapEntry(fourthKey, THIRD_VALUE)
    );

    Set<MapEntry> actualCheckedEntriesSet = checkedMapProof.getEntries();

    assertThat(
        actualCheckedEntriesSet,
        containsInAnyOrder(expectedEntriesList.toArray()));

    assertTrue(checkedMapProof.containsKey(firstKey));
    assertThat(checkedMapProof.get(firstKey), equalTo(FIRST_VALUE));
    assertThat(checkedMapProof.get(secondKey), equalTo(SECOND_VALUE));
    assertThat(checkedMapProof.get(fourthKey), equalTo(THIRD_VALUE));

    // If a user checks for key that wasn't required, an IllegalArgumentException is thrown

    assertThrows(IllegalArgumentException.class,
        () -> checkedMapProof.containsKey(ByteString.copyFromUtf8("not required key")));
  }

  @Test
  void mapProofWithOneElementShouldBeCorrect() {
    ByteString key = DbKeyTestUtils.keyByteStringFromString("01");
    ByteString value = FIRST_VALUE;
    MapEntry mapEntry = createMapEntry(key, value);

    HashCode valueHash = HASH_FUNCTION.hashByteString(value);
    HashCode expectedRootHash = HASH_FUNCTION.newHasher()
        .putObject(DbKey.newLeafKey(key), dbKeyFunnel())
        .putObject(valueHash, hashCodeFunnel())
        .hash();

    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(
            emptyList(),
            singletonList(mapEntry),
            emptyList());
    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();

    assertThat(checkedMapProof.getRootHash(), equalTo(expectedRootHash));
    assertTrue(checkedMapProof.compareWithRootHash(expectedRootHash));

    assertThat(checkedMapProof.getEntries(), equalTo(singleton(mapEntry)));
    assertTrue(checkedMapProof.containsKey(key));
    assertThat(checkedMapProof.get(key), equalTo(value));
  }

  @Test
  void mapProofWithEqualEntriesOrderShouldBeInvalid() {
    DbKey firstDbKey = DbKeyTestUtils.branchKeyFromPrefix("101100");
    DbKey secondKey = DbKeyTestUtils.branchKeyFromPrefix("101100");
    List<MapProofEntry> entries = Arrays.asList(
        createMapProofEntry(firstDbKey),
        createMapProofEntry(secondKey)
    );
    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(entries, emptyList(), emptyList());

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(checkedMapProof.getProofStatus(), equalTo(MapProofStatus.DUPLICATE_PATH));
  }

  @Test
  void mapProofWithoutEntriesShouldBeCorrect() {
    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(
            emptyList(), emptyList(), emptyList());

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(checkedMapProof.getProofStatus(), equalTo(MapProofStatus.CORRECT));
  }

  @Test
  void mapProofWithAbsentKeyShouldBeCorrect() {
    DbKey firstDbKey = DbKeyTestUtils.branchKeyFromPrefix("101100");
    ByteString valueKey = DbKeyTestUtils.keyByteStringFromString("101110");
    byte[] absentKey = DbKeyTestUtils.keyFromString("101111");

    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(
            singletonList(createMapProofEntry(firstDbKey)),
            singletonList(createMapEntry(valueKey, FIRST_VALUE)),
            singletonList(absentKey));

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(checkedMapProof.getProofStatus(), equalTo(MapProofStatus.CORRECT));
  }

  @Test
  void mapProofWithInvalidOrderShouldBeIncorrect() {
    DbKey firstDbKey = DbKeyTestUtils.branchKeyFromPrefix("10");
    DbKey secondDbKey = DbKeyTestUtils.branchKeyFromPrefix("01");

    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(
            Arrays.asList(createMapProofEntry(firstDbKey), createMapProofEntry(secondDbKey)),
            emptyList(),
            emptyList());

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(checkedMapProof.getProofStatus(), equalTo(MapProofStatus.INVALID_ORDER));
  }

  @Test
  void mapProofWithSingleBranchProofEntryShouldBeInvalid() {
    DbKey firstDbKey = DbKeyTestUtils.branchKeyFromPrefix("1011111");
    byte[] absentKey = DbKeyTestUtils.keyFromString("101111");

    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(
            singletonList(createMapProofEntry(firstDbKey)),
            emptyList(),
            singletonList(absentKey));

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(checkedMapProof.getProofStatus(), equalTo(MapProofStatus.NON_TERMINAL_NODE));
  }

  @Test
  void mapProofWithSingleLeafProofEntryShouldBeCorrect() {
    DbKey firstDbKey = DbKeyTestUtils.leafKeyFromPrefix("1011111");
    byte[] absentKey = DbKeyTestUtils.keyFromString("101111");

    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(
            singletonList(createMapProofEntry(firstDbKey)),
            emptyList(),
            singletonList(absentKey));

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(checkedMapProof.getProofStatus(), equalTo(MapProofStatus.CORRECT));
  }

  @Test
  void mapProofWithIncludedPrefixesShouldBeInvalid() {
    DbKey firstDbKey = DbKeyTestUtils.branchKeyFromPrefix("01");
    DbKey secondDbKey = DbKeyTestUtils.branchKeyFromPrefix("11");
    byte[] absentKey = DbKeyTestUtils.keyFromString("111111");

    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(
            Arrays.asList(
                createMapProofEntry(firstDbKey),
                createMapProofEntry(secondDbKey)),
            emptyList(),
            singletonList(absentKey));

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(checkedMapProof.getProofStatus(), equalTo(MapProofStatus.EMBEDDED_PATH));
  }

  @Test
  void mapProofWithIncludedBranchPrefixesShouldBeInvalid() {
    DbKey firstDbKey = DbKeyTestUtils.branchKeyFromPrefix("01");
    DbKey secondDbKey = DbKeyTestUtils.branchKeyFromPrefix("011");
    byte[] absentKey = DbKeyTestUtils.keyFromString("111111");

    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(
            Arrays.asList(
                createMapProofEntry(firstDbKey),
                createMapProofEntry(secondDbKey)),
            emptyList(),
            singletonList(absentKey));

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(checkedMapProof.getProofStatus(), equalTo(MapProofStatus.EMBEDDED_PATH));
  }

  private static MapProofEntry createMapProofEntry(DbKey dbKey) {
    return new MapProofEntry(dbKey, HashCode.fromBytes(dbKey.getKeySlice()));
  }

  private static MapEntry createMapEntry(ByteString key, ByteString value) {
    return new MapEntry(key, value);
  }
}
