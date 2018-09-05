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

package com.exonum.binding.storage.proofs.map.flat;

import static com.exonum.binding.hash.Funnels.hashCodeFunnel;
import static com.exonum.binding.storage.proofs.DbKeyFunnel.dbKeyFunnel;
import static java.util.stream.Collectors.toList;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.HashFunction;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.storage.proofs.map.DbKey;
import com.exonum.binding.storage.proofs.map.DbKey.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An unchecked flat map proof, which does not include any intermediate nodes.
 */
public class UncheckedFlatMapProof implements UncheckedMapProof {

  private static final HashFunction HASH_FUNCTION = Hashing.defaultHashFunction();

  private final List<MapProofEntry> proofList = new ArrayList<>();

  private final List<MapProofEntry> proof;

  private final List<MapEntry> entries;

  private final List<byte[]> missingKeys;

  UncheckedFlatMapProof(
      List<MapProofEntry> proof,
      List<MapEntry> entries,
      List<byte[]> missingKeys) {
    this.proof = proof;
    this.entries = entries;
    this.missingKeys = missingKeys;
  }

  @SuppressWarnings("unused") // Native API
  static UncheckedFlatMapProof fromUnsorted(
      MapProofEntry[] proofList,
      MapEntry[] entries,
      byte[][] missingKeys) {
    List<MapProofEntry> proof = Arrays.asList(proofList);
    List<MapEntry> entriesList = Arrays.asList(entries);
    List<byte[]> missingKeysList = Arrays.asList(missingKeys);
    return new UncheckedFlatMapProof(proof, entriesList, missingKeysList);
  }

  @Override
  public CheckedMapProof check() {
    ProofStatus orderCheckResult = orderCheck();
    if (orderCheckResult != ProofStatus.CORRECT) {
      return CheckedFlatMapProof.invalid(orderCheckResult);
    }
    if (prefixesIncluded()) {
      return CheckedFlatMapProof.invalid(ProofStatus.INVALID_STRUCTURE);
    }
    if (isEmptyProof()) {
      return checkEmptyProof();
    } else if (isSingletonProof()) {
      return checkSingletonProof();
    } else {
      return checkProof();
    }
  }

  /**
   * Checks that all entries in the proof are in the valid order.
   *
   * <p>The keys must be in ascending order as defined by
   * the {@linkplain DbKey#compareTo(DbKey) comparator}; there must not be duplicates.
   *
   * @return {@code ProofStatus.CORRECT} if every following key is greater than the previous
   *         {@code ProofStatus.INVALID_ORDER} if any following key key is lesser than the previous
   *         {@code ProofStatus.DUPLICATE_PATH} if there are two equal keys
   * @see DbKey#compareTo(DbKey)
   */
  private ProofStatus orderCheck() {
    for (int i = 1; i < proof.size(); i++) {
      DbKey key = proof.get(i - 1).getDbKey();
      DbKey nextKey = proof.get(i).getDbKey();
      int comparisonResult = nextKey.compareTo(key);
      if (comparisonResult < 0) {
        return ProofStatus.INVALID_ORDER;
      } else if (comparisonResult == 0) {
        return ProofStatus.DUPLICATE_PATH;
      }
    }
    return ProofStatus.CORRECT;
  }

  /**
   * Check if any entry has a prefix among the paths in the proof entries. Both found and absent
   * keys are checked.
   */
  private boolean prefixesIncluded() {
    Stream<DbKey> requestedKeys =
        Stream.concat(
            entries.stream()
                .map(MapEntry::getKey),
            missingKeys.stream())
        .map(DbKey::newLeafKey);

    // TODO: proof entries are checked to be sorted at this stage, so it's possible …
    // to use binary search here
    return requestedKeys
        .anyMatch(leafEntryKey -> proof.stream()
            .map(MapProofEntry::getDbKey)
            .anyMatch(proofEntryKey ->
                proofEntryKey.isPrefixOf(leafEntryKey))
        );
  }

  private boolean isEmptyProof() {
    return proof.size() + entries.size() == 0;
  }

  private CheckedMapProof checkEmptyProof() {
    return CheckedFlatMapProof.correct(
        getEmptyProofListHash(), Collections.emptyList(), missingKeys);
  }

  private boolean isSingletonProof() {
    return proof.size() + entries.size() == 1;
  }

  private CheckedMapProof checkSingletonProof() {
    if (proof.size() == 1) {
      // There are no entries, therefore, the proof node must correspond to a leaf.
      MapProofEntry entry = proof.get(0);
      DbKey.Type nodeType = entry.getDbKey().getNodeType();
      if (nodeType == Type.BRANCH) {
        return CheckedFlatMapProof.invalid(ProofStatus.NON_TERMINAL_NODE);
      } else {
        return CheckedFlatMapProof.correct(getSingleEntryProofHash(entry), entries, missingKeys);
      }
    } else {
      // The proof consists of a single leaf with a required key
      MapEntry entry = entries.get(0);
      return CheckedFlatMapProof.correct(getMapEntryHash(entry), entries, missingKeys);
    }
  }

  private CheckedMapProof checkProof() {
    mergeLeavesWithBranches();
    Deque<MapProofEntry> contour = new ArrayDeque<>();
    MapProofEntry first = proofList.get(0);
    MapProofEntry second = proofList.get(1);
    DbKey lastPrefix = first.getDbKey().commonPrefix(second.getDbKey());
    contour.push(first);
    contour.push(second);
    for (int i = 2; i < proofList.size(); i++) {
      MapProofEntry currentEntry = proofList.get(i);
      DbKey newPrefix = contour.peek().getDbKey().commonPrefix(currentEntry.getDbKey());
      while (contour.size() > 1
          && newPrefix.getNumSignificantBits() < lastPrefix.getNumSignificantBits()) {
        lastPrefix = fold(contour, lastPrefix).orElse(lastPrefix);
      }
      contour.push(currentEntry);
      lastPrefix = newPrefix;
    }
    while (contour.size() > 1) {
      lastPrefix = fold(contour, lastPrefix).orElse(lastPrefix);
    }
    return CheckedFlatMapProof.correct(contour.peek().getHash(), entries, missingKeys);
  }

  /**
   * Compute hashes of leaf entries and merge them into list of branches.
   */
  private void mergeLeavesWithBranches() {
    proofList.addAll(proof);
    List<MapProofEntry> leafEntries =
        entries
            .stream()
            .map(e -> new MapProofEntry(DbKey.newLeafKey(e.getKey()), getMapEntryHash(e)))
            .collect(toList());
    proofList.addAll(leafEntries);
    proofList.sort(Comparator.comparing(MapProofEntry::getDbKey));
  }

  /**
   * Folds two last entries in a contour and replaces them with the folded entry.
   * Returns an updated common prefix between two last entries in the contour.
   */
  private Optional<DbKey> fold(Deque<MapProofEntry> contour, DbKey lastPrefix) {
    MapProofEntry lastEntry = contour.pop();
    MapProofEntry penultimateEntry = contour.pop();
    MapProofEntry newEntry =
        new MapProofEntry(lastPrefix, computeBranchHash(penultimateEntry, lastEntry));
    Optional<DbKey> commonPrefix;
    if (!contour.isEmpty()) {
      MapProofEntry previousEntry = contour.peek();
      commonPrefix = Optional.of(previousEntry.getDbKey().commonPrefix(lastPrefix));
    } else {
      commonPrefix = Optional.empty();
    }

    contour.push(newEntry);
    return commonPrefix;
  }

  private static HashCode getEmptyProofListHash() {
    return HashCode.fromBytes(new byte[Hashing.DEFAULT_HASH_SIZE_BYTES]);
  }

  private static HashCode getSingleEntryProofHash(MapProofEntry entry) {
    return HASH_FUNCTION.newHasher()
        .putObject(entry.getDbKey(), dbKeyFunnel())
        .putObject(entry.getHash(), hashCodeFunnel())
        .hash();
  }

  private static HashCode getMapEntryHash(MapEntry entry) {
    return HASH_FUNCTION.hashBytes(entry.getValue());
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
}
