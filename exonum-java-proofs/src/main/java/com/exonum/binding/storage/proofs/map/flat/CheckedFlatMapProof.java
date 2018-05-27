package com.exonum.binding.storage.proofs.map.flat;

import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.storage.proofs.map.DbKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A checked flat map proof, which does not include any intermediate nodes.
 */
public class CheckedFlatMapProof implements CheckedMapProof {

  private List<MapProofEntryLeaf> proofList;

  @Nullable
  private HashCode rootHash;

  private ProofStatus status;

  CheckedFlatMapProof(ProofStatus status) {
    checkState(
        status != ProofStatus.CORRECT,
        "In case of valid status root hash and proof list should be passed");
    this.status = status;
  }

  CheckedFlatMapProof(ProofStatus status, HashCode rootHash, List<MapProofEntryLeaf> proofList) {
    checkState(
        status == ProofStatus.CORRECT,
        "In case of invalid status root hash and proof list should not be passed");
    this.status = status;
    this.rootHash = rootHash;
    this.proofList = new ArrayList<>(proofList);
  }

  @Override
  public List<MapProofEntryLeaf> getEntries() {
    checkState(status == ProofStatus.CORRECT, "Proof is not valid: %s", status);
    return proofList;
  }

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

  @Override
  public HashCode getMerkleRoot() {
    checkState(status == ProofStatus.CORRECT, "Proof is not valid: %s", status);
    checkState(rootHash != null, "Root hash wasn't computed");
    return rootHash;
  }

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

  @Override
  public ProofStatus getStatus() {
    return status;
  }
}
