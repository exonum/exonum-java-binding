/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.common.proofs.map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;

import com.exonum.binding.common.hash.HashCode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * A checked flat map proof, which does not include any intermediate nodes.
 */
public class CheckedFlatMapProof implements CheckedMapProof {

  private final List<MapEntry> entries;

  private final List<byte[]> missingKeys;

  private final HashCode rootHash;

  private final MapProofStatus status;

  private CheckedFlatMapProof(
      MapProofStatus status,
      HashCode rootHash,
      List<MapEntry> entries,
      List<byte[]> missingKeys) {
    this.status = checkNotNull(status);
    this.rootHash = checkNotNull(rootHash);
    this.entries = checkNotNull(entries);
    this.missingKeys = checkNotNull(missingKeys);
  }

  /**
   * Creates a valid map proof.
   *
   * @param rootHash the Merkle root hash calculated by the validator
   * @param entries the list of entries that are proved to be in the map
   * @param missingKeys the list of keys that are proved <em>not</em> to be in the map
   * @return a new checked proof
   */
  public static CheckedFlatMapProof correct(
      HashCode rootHash,
      List<MapEntry> entries,
      List<byte[]> missingKeys) {
    return new CheckedFlatMapProof(MapProofStatus.CORRECT, rootHash, entries, missingKeys);
  }

  /**
   * Creates an invalid map proof.
   *
   * @param status the status explaining why the proof is not valid;
   *     must not be {@link MapProofStatus#CORRECT}
   * @return a new checked proof
   */
  public static CheckedFlatMapProof invalid(MapProofStatus status) {
    checkArgument(status != MapProofStatus.CORRECT);
    return new CheckedFlatMapProof(
        status, HashCode.fromInt(1), emptyList(), emptyList());
  }

  @Override
  public List<MapEntry> getEntries() {
    checkValid();
    return entries;
  }

  @Override
  public List<byte[]> getMissingKeys() {
    checkValid();
    return missingKeys;
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
        .map(MapEntry::getValue)
        .findFirst()
        .orElse(null);
  }

  @Override
  public MapProofStatus getStatus() {
    return status;
  }

  @Override
  public boolean compareWithRootHash(HashCode expectedRootHash) {
    checkValid();
    return rootHash.equals(expectedRootHash);
  }

  private void checkValid() {
    checkState(status == MapProofStatus.CORRECT, "Proof is not valid: %s", status);
  }

  private void checkThatKeyIsRequested(byte[] key) {
    Stream.concat(
        entries.stream().map(MapEntry::getKey),
        missingKeys.stream())
        .filter(entryKey -> Arrays.equals(entryKey, key))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Key that wasn't among requested keys was checked"));
  }
}
