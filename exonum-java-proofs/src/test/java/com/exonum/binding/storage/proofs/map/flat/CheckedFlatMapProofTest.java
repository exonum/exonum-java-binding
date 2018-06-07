package com.exonum.binding.storage.proofs.map.flat;

import static com.exonum.binding.hash.Funnels.hashCodeFunnel;
import static com.exonum.binding.storage.proofs.DbKeyFunnel.dbKeyFunnel;
import static com.exonum.binding.test.Bytes.bytes;
import static com.exonum.binding.test.Bytes.createPrefixed;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.HashFunction;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.storage.proofs.map.DbKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class CheckedFlatMapProofTest {

  private static final byte[] VALUE = "testValue".getBytes();
  private static final HashFunction HASH_FUNCTION = Hashing.defaultHashFunction();

  @Test
  public void mapProofShouldBeValid() {
    byte[] firstKey = createPrefixed(bytes(0b001101), DbKey.KEY_SIZE);
    byte[] secondKey = createPrefixed(bytes(0b011101), DbKey.KEY_SIZE);
    byte[] thirdKey = createPrefixed(bytes(0b1111101), DbKey.KEY_SIZE);

    DbKey valueKey = DbKey.newLeafKey(secondKey);
    MapProofEntryLeaf leaf = createLeafMapEntry(valueKey.getKeySlice(), VALUE);
    List<MapProofEntry> entries =
        Arrays.asList(
            createBranchMapEntry(firstKey),
            leaf,
            createBranchMapEntry(thirdKey));
    UncheckedMapProof uncheckedFlatMapProof = new UncheckedFlatMapProof(entries);

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(entries, equalTo(uncheckedFlatMapProof.getProofList()));

    CheckedMapProofEntry checkedEntry = new CheckedMapProofEntry(valueKey.getKeySlice(), VALUE);
    assertThat(Collections.singletonList(checkedEntry), equalTo(checkedMapProof.getEntries()));
    assertTrue(checkedMapProof.containsKey(valueKey.getKeySlice()));
    assertThat(VALUE, equalTo(checkedMapProof.get(valueKey.getKeySlice())));
  }

  @Test
  public void mapProofWithOneElementShouldBeValid() {
    byte[] key = createPrefixed(bytes(0b10), DbKey.KEY_SIZE);
    DbKey valueKey = DbKey.newLeafKey(key);
    HashCode expectedRootHash = HASH_FUNCTION
        .newHasher()
        .putObject(valueKey, dbKeyFunnel())
        .putObject(HASH_FUNCTION.hashBytes(VALUE), hashCodeFunnel())
        .hash();
    List<MapProofEntry> entries =
        Collections.singletonList(createLeafMapEntry(valueKey.getKeySlice(), VALUE));
    UncheckedMapProof uncheckedFlatMapProof = new UncheckedFlatMapProof(entries);
    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();

    assertThat(expectedRootHash, equalTo(checkedMapProof.getMerkleRoot()));

    CheckedMapProofEntry checkedEntry = new CheckedMapProofEntry(valueKey.getKeySlice(), VALUE);
    assertThat(Collections.singletonList(checkedEntry), equalTo(checkedMapProof.getEntries()));
    assertTrue(checkedMapProof.containsKey(valueKey.getKeySlice()));
    assertThat(VALUE, equalTo(checkedMapProof.get(valueKey.getKeySlice())));
  }

  @Test
  public void mapProofWithEqualEntriesOrderShouldBeInvalid() {
    byte[] firstKey = createPrefixed(bytes(0b001101), DbKey.KEY_SIZE);
    byte[] secondKey = createPrefixed(bytes(0b001101), DbKey.KEY_SIZE);
    List<MapProofEntry> entries =
        Arrays.asList(
            createBranchMapEntry(firstKey),
            createBranchMapEntry(secondKey));
    UncheckedMapProof uncheckedFlatMapProof = new UncheckedFlatMapProof(entries);

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(checkedMapProof.getStatus(), equalTo(ProofStatus.DUPLICATE_PATH));
  }

  @Test
  public void mapProofWithWrongEntriesOrderShouldBeInvalid() {
    byte[] firstKey = createPrefixed(bytes(0b011101), DbKey.KEY_SIZE);
    byte[] secondKey = createPrefixed(bytes(0b001101), DbKey.KEY_SIZE);
    List<MapProofEntry> entries =
        Arrays.asList(
            createBranchMapEntry(firstKey),
            createLeafMapEntry(secondKey, VALUE));
    UncheckedMapProof uncheckedFlatMapProof = new UncheckedFlatMapProof(entries);

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(checkedMapProof.getStatus(), equalTo(ProofStatus.INVALID_ORDER));
  }

  private static MapProofEntryBranch createBranchMapEntry(byte[] key) {
    byte[] prefixedKey = createPrefixed(key, DbKey.KEY_SIZE);
    return new MapProofEntryBranch(
        DbKey.newBranchKey(prefixedKey, key.length),
        HashCode.fromBytes(key));
  }

  private static MapProofEntryLeaf createLeafMapEntry(byte[] key, byte[] value) {
    byte[] prefixedKey = createPrefixed(key, DbKey.KEY_SIZE);
    return new MapProofEntryLeaf(
        DbKey.newLeafKey(prefixedKey),
        value,
        HASH_FUNCTION);
  }
}
