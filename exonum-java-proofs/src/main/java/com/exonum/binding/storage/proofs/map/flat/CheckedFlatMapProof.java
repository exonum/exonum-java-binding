package com.exonum.binding.storage.proofs.map.flat;

import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.storage.proofs.map.DbKey;
import com.exonum.binding.storage.proofs.map.DbKey.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A flat map proof, which does not include any intermediate nodes.
 */
public class CheckedFlatMapProof implements CheckedMapProof {

  private List<MapProofEntry> proofList;

  @Nullable
  private HashCode rootHash;

  @Nullable
  private ProofStatus status;

  CheckedFlatMapProof(ProofStatus status) {
    checkState(
        status != ProofStatus.CORRECT,
        "In case of valid status root hash and proof list should be passed");
    this.status = status;
  }

  CheckedFlatMapProof(ProofStatus status, HashCode rootHash, List<MapProofEntry> proofList) {
    checkState(
        status == ProofStatus.CORRECT,
        "In case of invalid status root hash and proof list should not be passed");
    this.status = status;
    this.rootHash = rootHash;
    this.proofList = new ArrayList<>(proofList);
  }

  @Override
  public List<MapProofEntry> getProofList() {
    checkState(status == ProofStatus.CORRECT, "Proof is not valid: %s", status);
    return proofList;
  }

  /**
   * Checks if a leaf entry contains a key.
   */
  @Override
  public boolean containsKey(byte[] key) {
    checkState(status == ProofStatus.CORRECT, "Proof is not valid: %s", status);
    for (MapProofEntry entry: proofList) {
      DbKey entryKey = entry.getDbKey();
      if (entry instanceof MapProofEntryLeaf && Arrays.equals(entryKey.getKeySlice(), key)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if a hash of a proof root node equals to expected root hash.
   */
  @Override
  public boolean isValid(HashCode expectedRootHash) {
    checkState(status == ProofStatus.CORRECT, "Proof is not valid: %s", status);
    checkState(rootHash != null, "Root hash wasn't computed");
    return rootHash.equals(expectedRootHash);
  }

  /**
   * Get a value, corresponding to specified key.
   */
  @Override
  public byte[] get(byte[] key) {
    checkState(status == ProofStatus.CORRECT, "Proof is not valid: %s", status);
    for (MapProofEntry entry: proofList) {
      DbKey entryKey = entry.getDbKey();
      if (entry instanceof MapProofEntryLeaf && Arrays.equals(entryKey.getKeySlice(), key)) {
        return ((MapProofEntryLeaf) entry).getValue();
      }
    }
    return null;
  }

  /**
   * Get status of this proof.
   */
  @Override
  public ProofStatus getStatus() {
    return status;
  }
}
