package com.exonum.binding.storage.proofs.map.flat;

import static com.exonum.binding.hash.Funnels.hashCodeFunnel;
import static com.exonum.binding.storage.proofs.DbKeyFunnel.dbKeyFunnel;
import static com.exonum.binding.test.Bytes.bytes;
import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.HashFunction;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.storage.proofs.map.DbKey;
import com.exonum.binding.storage.proofs.map.DbKey.Type;
import com.exonum.binding.storage.proofs.map.KeyBitSet;
import com.google.common.primitives.UnsignedBytes;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.Test;

public class FlatMapProofTest {

  private static final byte[] VALUE = "testValue".getBytes();
  private static final HashFunction HASH_FUNCTION = Hashing.defaultHashFunction();

  @Test
  public void MapProofShouldBeValid() {
    KeyBitSet firstKey = new KeyBitSet(bytes(0x2D), 6);
    KeyBitSet secondKey = new KeyBitSet(bytes(0x3D), 6);
    KeyBitSet thirdKey = new KeyBitSet(bytes(0x1D), 5);

    DbKey valueKey = createDbKey(Type.LEAF.code, secondKey);
    MapProof flatMapProof =
        new FlatMapProof(
            Arrays.asList(
                createMapEntry(firstKey, Type.BRANCH, Optional.empty()),
                createMapEntry(secondKey, Type.LEAF, Optional.of(VALUE)),
                createMapEntry(thirdKey, Type.BRANCH, Optional.empty())));

//    assertTrue(flatMapProof.isValid(expectedRootHash));
    ((FlatMapProof) flatMapProof).check();
    assertTrue(flatMapProof.containsKey(valueKey.getKeySlice()));
    assertThat(VALUE, equalTo(flatMapProof.get(valueKey.getKeySlice()).get()));
  }

  @Test
  public void MapProofWithOneElementShouldBeValid() {
    KeyBitSet key = new KeyBitSet(bytes(0x2), 2);
    DbKey dbKey = createDbKey(Type.LEAF.code, key);
    HashCode expectedRootHash = HASH_FUNCTION
        .newHasher()
        .putObject(dbKey, dbKeyFunnel())
        .putObject(HASH_FUNCTION.hashBytes(VALUE), hashCodeFunnel())
        .hash();
    MapProof flatMapProof =
        new FlatMapProof(Collections.singletonList(createMapEntry(key, Type.LEAF, Optional.of(VALUE))));
    assertTrue(flatMapProof.isValid(expectedRootHash));
    assertTrue(flatMapProof.containsKey(dbKey.getKeySlice()));
    assertThat(VALUE, equalTo(flatMapProof.get(dbKey.getKeySlice()).get()));
  }

  private static DbKey createDbKey(int type, KeyBitSet keyBitSet) {
    byte[] rawDbKey = new byte[DbKey.DB_KEY_SIZE];
    rawDbKey[0] = UnsignedBytes.checkedCast(type);
    System.arraycopy(
        keyBitSet.getKeyBits().toByteArray(),
        0,
        rawDbKey,
        1,
        Math.min(DbKey.KEY_SIZE, keyBitSet.getKeyBits().toByteArray().length));
    int numSignificantBits;
    if (type == Type.BRANCH.code) {
      numSignificantBits = keyBitSet.getLength() * Byte.SIZE;
    } else {
      numSignificantBits = 0;
    }
    rawDbKey[DbKey.DB_KEY_SIZE - 1] = UnsignedBytes.checkedCast(numSignificantBits);
    return DbKey.fromBytes(rawDbKey);
  }

  private static MapProofEntry createMapEntry(
      KeyBitSet keyBitSet, Type type, Optional<byte[]> value) {
    if (type == Type.BRANCH) {
      return new MapProofEntryBranch(
          createDbKey(Type.BRANCH.code, keyBitSet),
          HashCode.fromBytes(keyBitSet.getKeyBits().toByteArray()));
    } else {
      checkArgument(value.isPresent());
      return new MapProofEntryLeaf(
          createDbKey(Type.LEAF.code, keyBitSet),
          value.get(),
          HASH_FUNCTION);
    }
  }

}
