package com.exonum.binding.storage.proofs.map.flat;

import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.hash.HashCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A checked flat map proof, which does not include any intermediate nodes.
 */
public class CheckedFlatMapProof implements CheckedMapProof {

  @Nullable
  private List<CheckedMapProofEntry> entryList;

  @Nullable
  private HashCode rootHash;

  private ProofStatus status;

  private CheckedFlatMapProof(
      ProofStatus status, HashCode rootHash, List<CheckedMapProofEntry> entryList) {
    this.status = status;
    this.rootHash = rootHash;
    this.entryList = new ArrayList<>(entryList);
  }

  static CheckedFlatMapProof correct(HashCode rootHash, List<CheckedMapProofEntry> proofList) {
    return new CheckedFlatMapProof(ProofStatus.CORRECT, rootHash, proofList);
  }

  static CheckedFlatMapProof invalid(ProofStatus status) {
    return new CheckedFlatMapProof(status, HashCode.fromInt(1), Collections.emptyList());
  }

  @Override
  public List<CheckedMapProofEntry> getEntries() {
    checkValid();
    return entryList;
  }

  @Override
  public boolean containsKey(byte[] key) {
    checkValid();
    for (CheckedMapProofEntry entry: entryList) {
      if (Arrays.equals(entry.getKey(), key)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public HashCode getMerkleRoot() {
    checkValid();
    return rootHash;
  }

  @Override
  public byte[] get(byte[] key) {
    checkValid();
    for (CheckedMapProofEntry entry: entryList) {
      if (Arrays.equals(entry.getKey(), key)) {
        return entry.getValue();
      }
    }
    return null;
  }

  @Override
  public ProofStatus getStatus() {
    return status;
  }

  @Override
  public boolean compareWithRootHash(HashCode expectedRootHash) {
    return status == ProofStatus.CORRECT && rootHash.equals(expectedRootHash);
  }

  private void checkValid() {
    checkState(status == ProofStatus.CORRECT, "Proof is not valid: %s", status);
  }
}
