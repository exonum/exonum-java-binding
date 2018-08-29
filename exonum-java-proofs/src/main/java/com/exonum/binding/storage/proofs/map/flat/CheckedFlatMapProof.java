package com.exonum.binding.storage.proofs.map.flat;

import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.hash.HashCode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A checked flat map proof, which does not include any intermediate nodes.
 */
public class CheckedFlatMapProof implements CheckedMapProof {

  private List<CheckedMapProofEntry> entries;

  private List<CheckedMapProofAbsentEntry> absentEntries;

  private HashCode rootHash;

  private ProofStatus status;

  private CheckedFlatMapProof(
      ProofStatus status,
      HashCode rootHash,
      List<CheckedMapProofEntry> entries,
      List<CheckedMapProofAbsentEntry> absentEntries) {
    this.status = status;
    this.rootHash = rootHash;
    this.entries = entries;
    this.absentEntries = absentEntries;
  }

  static CheckedFlatMapProof correct(
      HashCode rootHash,
      List<CheckedMapProofEntry> entries,
      List<CheckedMapProofAbsentEntry> absentEntries) {
    return new CheckedFlatMapProof(ProofStatus.CORRECT, rootHash, entries, absentEntries);
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
    checkValid();
    return absentEntries
        .stream()
        .map(CheckedMapProofAbsentEntry::getKey)
        .collect(Collectors.toList());
  }

  @Override
  public boolean containsKey(byte[] key) {
    checkValid();
    checkThatKeyIsRequested(key);
    return entries.stream().anyMatch(entry -> Arrays.equals(entry.getKey(), key));
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
    return entries
        .stream()
        .filter(entry -> Arrays.equals(entry.getKey(), key))
        .map(CheckedMapProofEntry::getValue)
        .findFirst()
        .orElse(null);
  }

  @Override
  public ProofStatus getStatus() {
    return status;
  }

  @Override
  public boolean compareWithRootHash(HashCode expectedRootHash) {
    checkValid();
    return rootHash.equals(expectedRootHash);
  }

  private void checkValid() {
    checkState(status == ProofStatus.CORRECT, "Proof is not valid: %s", status);
  }

  private void checkThatKeyIsRequested(byte[] key) {
    Stream.concat(
        entries.stream().map(CheckedMapProofEntry::getKey),
        absentEntries.stream().map(CheckedMapProofAbsentEntry::getKey))
        .filter(entryKey -> Arrays.equals(entryKey, key))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Key that wasn't among requested keys was checked"));
  }
}
