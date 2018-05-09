package com.exonum.binding.storage.proofs.map.flat;

import static com.exonum.binding.hash.Funnels.hashCodeFunnel;
import static com.exonum.binding.storage.proofs.DbKeyFunnel.dbKeyFunnel;
import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.HashFunction;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.storage.proofs.map.DbKey;
import com.exonum.binding.storage.proofs.map.DbKey.Type;
import com.exonum.binding.storage.proofs.map.KeyBitSet;
import com.google.common.primitives.UnsignedBytes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * A flat map proof, which does not include any intermediate nodes.
 */
public class FlatMapProof implements MapProof {

  private static final HashFunction HASH_FUNCTION = Hashing.defaultHashFunction();

  private final List<MapProofEntry> proofList;

  enum Status {
    VALID("Proof is correct"),
    BRANCH_ENTRY_IS_A_LEAF("Leaf entry is of branch type"),
    INVALID_STRUCTURE("Proof has invalid structure"),
    NOT_CHECKED("Proof is not yet checked");

    final String description;

    Status(String description) {
      this.description = description;
    }
  }

  private static class KeyValuePair {
    private DbKey key;
    private byte[] value;

    private KeyValuePair(DbKey key, byte[] value) {
      this.key = key;
      this.value = value;
    }
  }

  @Nullable
  private KeyValuePair keyValuePair;

  @Nullable
  private HashCode rootHash;

  @Nullable
  private Status status = Status.NOT_CHECKED;

  FlatMapProof(List<MapProofEntry> proofList) {
    this.proofList = proofList;
  }

  public List<MapProofEntry> getProofList() {
    return proofList;
  }

  public boolean containsKey(byte[] key) {
    for (MapProofEntry entry: proofList) {
      DbKey entryKey = entry.getDbKey();
      //TODO: key = [x], keySlice = [x, 0, 0, 0, ...]
      if (entryKey.getNodeType() == Type.LEAF && Arrays.equals(entryKey.getKeySlice(), key)) {
        return true;
      }
    }
    return false;
  }

  public boolean isValid(HashCode expectedRootHash) {
    if (status == Status.NOT_CHECKED) {
      check();
    }
    checkState(status == Status.VALID, "Proof is not valid: %s", status);
    checkState(rootHash != null, "Root hash wasn't computed");
    return rootHash.equals(expectedRootHash);
  }

  public Optional<byte[]> get(byte[] key) {
    checkState(status == Status.VALID, "Proof is not valid: %s", status);
    checkState(keyValuePair != null, "Value node wasn't found");
    //TODO: key = [x], keySlice = [x, 0, 0, 0, ...]
    if (Arrays.equals(keyValuePair.key.getKeySlice(), key)) {
      return Optional.of(keyValuePair.value);
    } else {
      return Optional.empty();
    }
  }

  // Not private for test purposes.
  void check() {
    if (proofList.isEmpty()) {
      rootHash = getEmptyProofListHash();
      status = Status.VALID;
    } else if (proofList.size() == 1) {
      MapProofEntry singleEntry = proofList.get(0);
      if (singleEntry.getDbKey().getNodeType() == Type.LEAF) {
        HashCode valueHash = singleEntry.getHash();
        setKeyValue(singleEntry.getDbKey(), ((MapProofEntryLeaf) singleEntry).getValue());
        rootHash = getSingletonProofListHash(singleEntry.getDbKey(), valueHash);
        status = Status.VALID;
      } else {
        status = Status.BRANCH_ENTRY_IS_A_LEAF;
      }
    } else {
      List<MapProofEntry> contour = new ArrayList<>();
      MapProofEntry first = proofList.get(0);
      //TODO: redo
      if (first.getDbKey().getNodeType() == Type.LEAF) {
        setKeyValue(first.getDbKey(), ((MapProofEntryLeaf) first).getValue());
      }
      MapProofEntry second = proofList.get(1);
      if (second.getDbKey().getNodeType() == Type.LEAF) {
        setKeyValue(second.getDbKey(), ((MapProofEntryLeaf) second).getValue());
      }
      DbKey lastPrefix;
      try {
        lastPrefix = getCommonKey(first, second);
      } catch (InvalidStructureException e) {
        status = Status.INVALID_STRUCTURE;
        return;
      }
      contour.add(first);
      contour.add(second);
      for (int i = 2; i < proofList.size(); i++) {
        if (proofList.get(i).getDbKey().getNodeType() == Type.LEAF) {
          setKeyValue(
              proofList.get(i).getDbKey(), ((MapProofEntryLeaf) proofList.get(i)).getValue());
        }
        DbKey newPrefix;
        try {
          newPrefix = getCommonKey(contour.get(contour.size() - 1), proofList.get(i));
        } catch (InvalidStructureException e) {
          status = Status.INVALID_STRUCTURE;
          return;
        }
        while (contour.size() > 1
            && newPrefix.getKeySlice().length < lastPrefix.getKeySlice().length) {
          lastPrefix = fold(contour, lastPrefix).orElse(lastPrefix);
        }
        contour.add(proofList.get(i));
        lastPrefix = newPrefix;
      }
      while (contour.size() > 1) {
        lastPrefix = fold(contour, lastPrefix).orElse(lastPrefix);
      }
      rootHash = contour.get(0).getHash();
      status = Status.VALID;
    }
  }

  private Optional<DbKey> fold(List<MapProofEntry> contour, DbKey lastPrefix) {
    MapProofEntry lastEntry = contour.remove(contour.size() - 1);
    MapProofEntry penultEntry = contour.remove(contour.size() - 1);
    MapProofEntry newEntry =
        new MapProofEntryBranch(lastPrefix, computeBranchHash(penultEntry, lastEntry));
    contour.add(newEntry);
    if (contour.size() > 1) {
      penultEntry = contour.get(contour.size() - 2);
      KeyBitSet keyBitSet = penultEntry.getDbKey().keyBits().commonPrefix(lastPrefix.keyBits());
      return Optional.of(
          createDbKey(
              Type.BRANCH.code, keyBitSet.getKeyBits().toByteArray(), keyBitSet.getLength()));
    }
    return Optional.empty();
  }

  private void setKeyValue(DbKey key, byte[] value) {
    checkState(this.keyValuePair == null, "Setting the value for the 2nd time");
    this.keyValuePair = new KeyValuePair(key, value);
  }

  private static HashCode computeBranchHash(MapProofEntry leftChild, MapProofEntry rightChild) {
    return HASH_FUNCTION
        .newHasher()
        .putObject(leftChild.getHash(), hashCodeFunnel())
        .putObject(rightChild.getHash(), hashCodeFunnel())
        .putObject(leftChild.getDbKey(), dbKeyFunnel())
        .putObject(rightChild.getDbKey(), dbKeyFunnel())
        .hash();
  }

  private static HashCode getEmptyProofListHash() {
    return HashCode.fromBytes(new byte[Hashing.DEFAULT_HASH_SIZE_BYTES]);
  }

  private static HashCode getSingletonProofListHash(DbKey dbKey, HashCode valueHash) {
    return HASH_FUNCTION.newHasher()
        .putObject(dbKey, dbKeyFunnel())
        .putObject(valueHash, hashCodeFunnel())
        .hash();
  }

  private static DbKey getCommonKey(MapProofEntry first, MapProofEntry second)
      throws InvalidStructureException {
    KeyBitSet resultKeySet = first.getDbKey().keyBits().commonPrefix(second.getDbKey().keyBits());
    if (resultKeySet.getLength() == 0) {
      throw new InvalidStructureException();
    }
    return createDbKey(
            Type.BRANCH.code,
            resultKeySet.getKeyBits().toByteArray(),
            resultKeySet.getLength());
  }

  // TODO: maybe move to utils
  private static DbKey createDbKey(int type, byte[] keySlice, int numSignificantBits) {
    byte[] rawDbKey = new byte[DbKey.DB_KEY_SIZE];
    rawDbKey[0] = UnsignedBytes.checkedCast(type);
    System.arraycopy(keySlice, 0, rawDbKey, 1, Math.min(DbKey.KEY_SIZE, keySlice.length));
    rawDbKey[DbKey.DB_KEY_SIZE - 1] = UnsignedBytes.checkedCast(numSignificantBits);
    return DbKey.fromBytes(rawDbKey);
  }
}
