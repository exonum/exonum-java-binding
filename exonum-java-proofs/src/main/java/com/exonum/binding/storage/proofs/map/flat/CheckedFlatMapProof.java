package com.exonum.binding.storage.proofs.map.flat;

import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.hash.HashCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A checked flat map proof, which does not include any intermediate nodes.
 */
public class CheckedFlatMapProof implements CheckedMapProof {

  // TODO: make all entries as current branch entries and keep keys separate
  private List<CheckedMapProofEntry> entries;

  private List<byte[]> requestedKeys;

  private List<byte[]> missingKeys;

  private HashCode rootHash;

  private ProofStatus status;

  private CheckedFlatMapProof(
      ProofStatus status,
      HashCode rootHash,
      List<CheckedMapProofEntry> entries,
      List<byte[]> requestedKeys) {
    this.status = status;
    this.rootHash = rootHash;
    this.entries = new ArrayList<>(entries);
    this.requestedKeys = requestedKeys;
    this.missingKeys =
        determineMissingKeys(
            requestedKeys,
            entries.stream().map(CheckedMapProofEntry::getKey).collect(Collectors.toList()));
  }

  static CheckedFlatMapProof correct(
      HashCode rootHash, List<CheckedMapProofEntry> proofList, List<byte[]> requestedKeys) {
    return new CheckedFlatMapProof(ProofStatus.CORRECT, rootHash, proofList, requestedKeys);
  }

  static CheckedFlatMapProof invalid(ProofStatus status) {
    return new CheckedFlatMapProof(
        status, HashCode.fromInt(1), Collections.emptyList(), Collections.emptyList());
  }

  @Override
  public List<CheckedMapProofEntry> getEntries() {
    checkValid();
    return entries;
  }

  @Override
  public List<byte[]> getMissingKeys() {
    return missingKeys;
  }

  @Override
  public boolean containsKey(byte[] key) {
    checkValid();
    checkThatKeyIsRequested(key);
    for (CheckedMapProofEntry entry: entries) {
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
    for (CheckedMapProofEntry entry: entries) {
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

  static List<byte[]> determineMissingKeys(List<byte[]> requestedKeys, List<byte[]> foundKeys) {
    List<byte[]> missingKeys = new ArrayList<>(requestedKeys);
    missingKeys.removeAll(foundKeys);
    return missingKeys;
  }
}
