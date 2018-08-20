package com.exonum.binding.storage.proofs.map.flat;

import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.hash.HashCode;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A checked flat map proof, which does not include any intermediate nodes.
 */
public class CheckedFlatMapProof implements CheckedMapProof {

  private Set<CheckedMapProofEntry> entryList;

  private Set<byte[]> requestedKeys;

  private Set<byte[]> missingKeys;

  private HashCode rootHash;

  private ProofStatus status;

  private CheckedFlatMapProof(
      ProofStatus status,
      HashCode rootHash,
      Set<CheckedMapProofEntry> entryList,
      Set<byte[]> requestedKeys) {
    this.status = status;
    this.rootHash = rootHash;
    this.entryList = new HashSet<>(entryList);
    this.requestedKeys = requestedKeys;
    this.missingKeys =
        determineMissingKeys(
            requestedKeys,
            entryList.stream().map(CheckedMapProofEntry::getKey).collect(Collectors.toSet()));
  }

  static CheckedFlatMapProof correct(
      HashCode rootHash, Set<CheckedMapProofEntry> proofList, Set<byte[]> requestedKeys) {
    return new CheckedFlatMapProof(ProofStatus.CORRECT, rootHash, proofList, requestedKeys);
  }

  static CheckedFlatMapProof invalid(ProofStatus status) {
    return new CheckedFlatMapProof(
        status, HashCode.fromInt(1), Collections.emptySet(), Collections.emptySet());
  }

  @Override
  public Set<CheckedMapProofEntry> getEntries() {
    checkValid();
    return entryList;
  }

  @Override
  public Set<byte[]> getMissingKeys() {
    return missingKeys;
  }

  @Override
  public boolean containsKey(byte[] key) {
    checkValid();
    checkThatKeyIsRequested(key);
    for (CheckedMapProofEntry entry: entryList) {
      if (Arrays.equals(entry.getKey(), key)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public HashCode getRootHash() {
    checkValid();
    return rootHash;
  }

  @Override
  public byte[] get(byte[] key) {
    checkValid();
    checkThatKeyIsRequested(key);
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

  private void checkThatKeyIsRequested(byte[] key) {
    for (byte[] requestedKey: requestedKeys) {
      if (Arrays.equals(requestedKey, key)) {
        return;
      }
    }
    throw new IllegalArgumentException("Key that wasn't among requested keys was checked");
  }

  static Set<byte[]> determineMissingKeys(Set<byte[]> requestedKeys, Set<byte[]> foundKeys) {
    Set<byte[]> missingKeys = new HashSet<>(requestedKeys);
    missingKeys.removeAll(foundKeys);
    return missingKeys;
  }
}
