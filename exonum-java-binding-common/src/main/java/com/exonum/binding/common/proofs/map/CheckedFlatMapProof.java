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
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.exonum.binding.common.collect.MapEntry;
import com.exonum.binding.common.hash.HashCode;
import com.google.protobuf.ByteString;
import java.util.Map;
import java.util.Set;

/**
 * A checked flat map proof, which does not include any intermediate nodes.
 */
public class CheckedFlatMapProof implements CheckedMapProof {

  private final Map<ByteString, ByteString> entries;

  private final Set<ByteString> missingKeys;

  private final HashCode rootHash;

  private final MapProofStatus status;

  private CheckedFlatMapProof(
      MapProofStatus status,
      HashCode rootHash,
      Set<MapEntry<ByteString, ByteString>> entries,
      Set<ByteString> missingKeys) {
    this.status = checkNotNull(status);
    this.rootHash = checkNotNull(rootHash);
    this.entries = entries.stream()
        .collect(toMap(MapEntry::getKey, MapEntry::getValue));
    this.missingKeys = checkNotNull(missingKeys);
  }

  /**
   * Creates a valid map proof.
   *
   * @param rootHash the Merkle root hash calculated by the validator
   * @param entries the set of entries that are proved to be in the map
   * @param missingKeys the set of keys that are proved <em>not</em> to be in the map
   * @return a new checked proof
   */
  public static CheckedFlatMapProof correct(
      HashCode rootHash,
      Set<MapEntry<ByteString, ByteString>> entries,
      Set<ByteString> missingKeys) {
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
        status, HashCode.fromInt(1), emptySet(), emptySet());
  }

  @Override
  public Set<MapEntry<ByteString, ByteString>> getEntries() {
    checkValid();
    return entries.entrySet()
        .stream()
        .map(e -> com.exonum.binding.common.collect.MapEntry.valueOf(e.getKey(), e.getValue()))
        .collect(toSet());
  }

  @Override
  public Set<ByteString> getMissingKeys() {
    checkValid();
    return missingKeys;
  }

  @Override
  public boolean containsKey(ByteString key) {
    checkValid();
    checkThatKeyIsRequested(key);
    return entries.containsKey(key);
  }

  @Override
  public HashCode getRootHash() {
    checkValid();
    return rootHash;
  }

  @Override
  public ByteString get(ByteString key) {
    checkValid();
    checkThatKeyIsRequested(key);
    return entries.get(key);
  }

  @Override
  public MapProofStatus getProofStatus() {
    return status;
  }

  @Override
  public boolean isValid() {
    return status == MapProofStatus.CORRECT;
  }

  private void checkValid() {
    checkState(isValid(), "Proof is not valid: %s", status);
  }

  private void checkThatKeyIsRequested(ByteString key) {
    checkArgument(entries.containsKey(key) || missingKeys.contains(key),
        "Key (%s) that wasn't among requested keys was checked", key);
  }
}
