package com.exonum.binding.storage.proofs.map.flat;

import static com.exonum.binding.hash.Funnels.hashCodeFunnel;
import static com.exonum.binding.storage.proofs.DbKeyFunnel.dbKeyFunnel;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.HashFunction;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.storage.proofs.map.DbKey;
import com.exonum.binding.storage.proofs.map.DbKeyTestUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class UncheckedFlatMapProofTest {

  private static final byte[] FIRST_VALUE = "testValue".getBytes();
  private static final byte[] SECOND_VALUE = "anotherTestValue".getBytes();
  private static final byte[] THIRD_VALUE = "oneMoreTestValue".getBytes();

  private static final HashFunction HASH_FUNCTION = Hashing.defaultHashFunction();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void mapProofShouldBeValid() {
    DbKey firstDbKey = DbKeyTestUtils.branchKeyFromPrefix("101100");
    byte[] valueKey = DbKeyTestUtils.keyFromString("101110");
    DbKey thirdDbKey = DbKeyTestUtils.branchKeyFromPrefix("1011111");

    MapEntry leaf = createMapEntry(valueKey, FIRST_VALUE);
    List<MapProofEntry> branches = Arrays.asList(
        createMapProofEntry(firstDbKey),
        createMapProofEntry(thirdDbKey)
    );
    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(
            branches, Collections.singletonList(leaf), Collections.emptyList());

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();

    MapEntry expectedEntry = new MapEntry(valueKey, FIRST_VALUE);
    assertThat(checkedMapProof.getEntries(), equalTo(Collections.singletonList(expectedEntry)));
    assertTrue(checkedMapProof.containsKey(valueKey));
    assertThat(checkedMapProof.get(valueKey), equalTo(FIRST_VALUE));
  }

  @Test
  public void mapProofWithSeveralLeafsShouldBeValid() {
    byte[] firstKey = DbKeyTestUtils.keyFromString("0011_0101");
    byte[] secondKey = DbKeyTestUtils.keyFromString("0011_0110");
    DbKey thirdDbKey = DbKeyTestUtils.branchKeyFromPrefix("0100_0000");
    byte[] fourthKey = DbKeyTestUtils.keyFromString("1000_1101");

    List<MapEntry> leaves = Arrays.asList(
        createMapEntry(firstKey, FIRST_VALUE),
        createMapEntry(secondKey, SECOND_VALUE),
        createMapEntry(fourthKey, THIRD_VALUE)
    );

    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(
            Collections.singletonList(createMapProofEntry(thirdDbKey)),
            leaves,
            Collections.emptyList());

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();

    List<MapEntry> expectedEntriesList = Arrays.asList(
        new MapEntry(firstKey, FIRST_VALUE),
        new MapEntry(secondKey, SECOND_VALUE),
        new MapEntry(fourthKey, THIRD_VALUE)
    );

    List<MapEntry> actualCheckedEntriesList = checkedMapProof.getEntries();

    assertThat(
        actualCheckedEntriesList,
        containsInAnyOrder(expectedEntriesList.toArray()));

    assertTrue(checkedMapProof.containsKey(firstKey));
    assertThat(checkedMapProof.get(firstKey), equalTo(FIRST_VALUE));
    assertThat(checkedMapProof.get(secondKey), equalTo(SECOND_VALUE));
    assertThat(checkedMapProof.get(fourthKey), equalTo(THIRD_VALUE));

    // If a user checks for key that wasn't required, an IllegalArgumentException is thrown
    expectedException.expect(IllegalArgumentException.class);
    checkedMapProof.containsKey("not required key".getBytes());
  }

  @Test
  public void mapProofWithOneElementShouldBeValid() {
    byte[] key = DbKeyTestUtils.keyFromString("01");
    MapEntry mapEntry = createMapEntry(key, FIRST_VALUE);
    HashCode expectedRootHash = HASH_FUNCTION
        .newHasher()
        .putObject(DbKey.newLeafKey(key), dbKeyFunnel())
        .putObject(HASH_FUNCTION.hashBytes(FIRST_VALUE), hashCodeFunnel())
        .hash();
    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(
            Collections.emptyList(),
            Collections.singletonList(mapEntry),
            Collections.emptyList());
    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();

    assertThat(checkedMapProof.getRootHash(), equalTo(expectedRootHash));

    assertThat(checkedMapProof.getEntries(), equalTo(Collections.singletonList(mapEntry)));
    assertTrue(checkedMapProof.containsKey(key));
    assertThat(checkedMapProof.get(key), equalTo(FIRST_VALUE));
  }

  @Test
  public void mapProofWithEqualEntriesOrderShouldBeInvalid() {
    DbKey firstDbKey = DbKeyTestUtils.branchKeyFromPrefix("101100");
    DbKey secondKey = DbKeyTestUtils.branchKeyFromPrefix("101100");
    List<MapProofEntry> entries = Arrays.asList(
        createMapProofEntry(firstDbKey),
        createMapProofEntry(secondKey)
    );
    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(entries, Collections.emptyList(), Collections.emptyList());

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(checkedMapProof.getStatus(), equalTo(ProofStatus.DUPLICATE_PATH));
  }

  @Test
  public void mapProofWithoutEntriesShouldBeValid() {
    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(checkedMapProof.getStatus(), equalTo(ProofStatus.CORRECT));
  }

  @Test
  public void mapProofWithAbsentKeyShouldBeCorrect() {
    DbKey firstDbKey = DbKeyTestUtils.branchKeyFromPrefix("101100");
    byte[] valueKey = DbKeyTestUtils.keyFromString("101110");
    byte[] absentKey = DbKeyTestUtils.keyFromString("101111");

    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(
            Collections.singletonList(createMapProofEntry(firstDbKey)),
            Collections.singletonList(createMapEntry(valueKey, FIRST_VALUE)),
            Collections.singletonList(absentKey));

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(checkedMapProof.getStatus(), equalTo(ProofStatus.CORRECT));
  }

  @Test
  public void mapProofWithSingleBranchProofEntryShouldBeInvalid() {
    DbKey firstDbKey = DbKeyTestUtils.branchKeyFromPrefix("1011111");
    byte[] absentKey = DbKeyTestUtils.keyFromString("101111");

    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(
            Collections.singletonList(createMapProofEntry(firstDbKey)),
            Collections.emptyList(),
            Collections.singletonList(absentKey));

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(checkedMapProof.getStatus(), equalTo(ProofStatus.NON_TERMINAL_NODE));
  }

  @Test
  public void mapProofWithSingleLeafProofEntryShouldBeInvalid() {
    DbKey firstDbKey = DbKeyTestUtils.leafKeyFromPrefix("1011111");
    byte[] absentKey = DbKeyTestUtils.keyFromString("101111");

    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(
            Collections.singletonList(createMapProofEntry(firstDbKey)),
            Collections.emptyList(),
            Collections.singletonList(absentKey));

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(checkedMapProof.getStatus(), equalTo(ProofStatus.CORRECT));
  }

  @Test
  public void mapProofWithIncludedPrefixesShouldBeInvalid() {
    DbKey firstDbKey = DbKeyTestUtils.branchKeyFromPrefix("11");
    DbKey secondDbKey = DbKeyTestUtils.branchKeyFromPrefix("01");
    byte[] absentKey = DbKeyTestUtils.keyFromString("111111");

    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(
            Arrays.asList(
                createMapProofEntry(firstDbKey),
                createMapProofEntry(secondDbKey)),
            Collections.emptyList(),
            Collections.singletonList(absentKey));

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(checkedMapProof.getStatus(), equalTo(ProofStatus.INVALID_STRUCTURE));
  }

  private static MapProofEntry createMapProofEntry(DbKey dbKey) {
    return new MapProofEntry(dbKey, HashCode.fromBytes(dbKey.getKeySlice()));
  }

  private static MapEntry createMapEntry(byte[] key, byte[] value) {
    return new MapEntry(key, value);
  }
}
