/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.common.proofs.list;

import static com.exonum.binding.common.hash.Funnels.hashCodeFunnel;
import static com.exonum.binding.common.hash.Hashing.sha256;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static com.google.common.math.BigIntegerMath.log2;
import static java.util.stream.Collectors.toMap;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hasher;
import com.exonum.binding.common.hash.Hashing;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Functions;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A flat list proof. It proves that certain elements are present in a proof list
 * of a certain size.
 */
class FlatListProof {
  /*
    Proof lists are represented as BSTs, where all leaf elements are at the same height.
    Here is a tree for a three-element proof list:

     H — height of nodes at each level
     2        o
            /   \
     1    o       o
         / \     /
     0  e   e   e

    Proofs for elements of proof lists include the list size; the requested elements;
    and hashes of all sub-trees that are adjacent to the paths from the root node to the leaf nodes
    containing the elements.

    Here is a proof for the element at index 1 from the list shown above:

     H
     2        o
            /   \
     1    o       h
         / \
     0  h   e
     ________________
     Legend:
       e — element proof entry (ListProofElementEntry)
       h — hashed proof entry (ListProofHashedEntry)
       o — 'virtual' node — not present in the proof, inferred during verification.
           Shown mostly to communicate the tree structure of the proof.

    See also: https://wiki.bf.local/display/EXN/Flat+list+proofs
  */

  private static final long MAX_SIZE = ListProofEntry.MAX_INDEX + 1;

  private static final HashCode EMPTY_LIST_INDEX_HASH = hashListIndex(0L,
      HashCode.fromBytes(new byte[Hashing.DEFAULT_HASH_SIZE_BYTES]));

  @VisibleForTesting
  static final byte BLOB_PREFIX = 0x00;
  @VisibleForTesting
  static final byte LIST_BRANCH_PREFIX = 0x01;
  @VisibleForTesting
  static final byte LIST_ROOT_PREFIX = 0x02;

  private final List<ListProofElementEntry> elements;
  private final List<ListProofHashedEntry> proof;
  private final long size;

  FlatListProof(List<ListProofElementEntry> elements,
      List<ListProofHashedEntry> proof, long size) {
    this.elements = checkNotNull(elements);
    this.proof = checkNotNull(proof);
    this.size = size;
  }

  CheckedListProof<byte[]> verify() {
    // Check the size
    if (size < 0 || MAX_SIZE < size) {
      throw new InvalidProofException(String.format("Invalid size (%s), must be in range [0; 2^56]",
          size));
    }

    // Handle special cases
    if (size == 0) {
      // Empty list
      return verifyEmptyListProof();
    } else if (elements.isEmpty()) {
      // Empty range proof
      return verifyEmptyRangeProof();
    } else {
      // 1+ element proof
      return verifyNonEmptyListProof();
    }
  }

  private CheckedListProof<byte[]> verifyEmptyListProof() {
    // Check there are no elements or proof entries
    if (!elements.isEmpty()) {
      throw new InvalidProofException("Proof for empty list must not have elements, but has: "
          + elements);
    }
    if (!proof.isEmpty()) {
      throw new InvalidProofException(
          "Proof for empty list must not have proof entries, but has: " + proof);
    }
    return newCheckedProof(EMPTY_LIST_INDEX_HASH);
  }

  private CheckedListProof<byte[]> verifyEmptyRangeProof() {
    // Empty range: must have a single root hash node
    if (proof.size() != 1) {
      throw new InvalidProofException(String.format(
          "Proof for an empty range must have a single proof node, but has %d: %s",
          proof.size(), proof));
    }
    ListProofHashedEntry rootHashEntry = proof.get(0);
    int treeHeight = calcTreeHeight(size);
    // Check height
    if (rootHashEntry.getHeight() != treeHeight) {
      throw new InvalidProofException(
          String.format("Proof node for an empty range at invalid height (%d),"
              + "must be at height (%d) for a list of size %d: %s", rootHashEntry.getHeight(),
              treeHeight, size, rootHashEntry));
    }
    // Check index
    if (rootHashEntry.getIndex() != 0L) {
      throw new InvalidProofException(
          String.format("Proof node for an empty range at invalid index (%d),"
              + "must be always at index 0: %s", rootHashEntry.getIndex(), rootHashEntry));
    }
    HashCode rootHash = rootHashEntry.getHash();
    HashCode listHash = hashListIndex(rootHash);
    return newCheckedProof(listHash);
  }

  private CheckedListProof<byte[]> verifyNonEmptyListProof() {
    // Calculate the expected tree height
    int treeHeight = calcTreeHeight(size);

    // Index proof entries by height and verify their local correctness: no out-of-range nodes;
    // no duplicates.
    List<Map<Long, ListProofHashedEntry>> proofByHeight = indexHashedEntriesByHeight(treeHeight);

    // Check element entries: have unique indexes that are in range [0; size)
    checkElementEntries();

    // Compute the Merkle root hash
    HashCode rootHash = computeRootHash(proofByHeight, treeHeight);

    // Compute the list object hash
    HashCode indexHash = hashListIndex(rootHash);
    return newCheckedProof(indexHash);
  }

  @VisibleForTesting
  static int calcTreeHeight(long size) {
    return (size == 0L) ? 0
        : log2(BigInteger.valueOf(size), RoundingMode.CEILING);
  }

  /**
   * Indexes proof entries by their height, also verifying their local correctness:
   * no out-of-range nodes; no duplicates.
   *
   * @param treeHeight the height of the proof list tree
   * @return a list of proof entries at each height from 0 to treeHeight;
   *     entries at each level are indexed by their index
   */
  private List<Map<Long, ListProofHashedEntry>> indexHashedEntriesByHeight(int treeHeight) {
    List<Map<Long, ListProofHashedEntry>> proofByHeight = new ArrayList<>(treeHeight);
    for (int i = 0; i < treeHeight; i++) {
      // For single-element proofs, only a single proof node is expected on each level.
      // For contiguous range proofs, up to two proof nodes are expected on any level.
      // Multiple-range proofs might have up to 'elements.size' proof nodes on the lowest level,
      // but Exonum does not currently produce such.
      int initialCapacity = (elements.size() <= 1) ? 1 : 2;
      Map<Long, ListProofHashedEntry> proofAtHeight = newHashMapWithExpectedSize(initialCapacity);
      proofByHeight.add(proofAtHeight);
    }

    for (ListProofHashedEntry hashedEntry : proof) {
      // Check height
      int height = hashedEntry.getHeight();
      if (height < 0 || treeHeight <= height) {
        throw new InvalidProofException(
            String.format("Proof entry at invalid height (%d), must be in range [0; %d): %s",
                height, treeHeight, hashedEntry));
      }
      // Check index
      long levelSize = levelSizeAt(height);
      long index = hashedEntry.getIndex();
      if (index < 0L || levelSize <= index) {
        throw new InvalidProofException(String
            .format(
                "Proof entry at invalid index (%d); it must be in range [0; %d) at height %d: %s",
                index, levelSize, height, hashedEntry));
      }
      // Add the entry at the height, checking for duplicates
      Map<Long, ListProofHashedEntry> proofsAtHeight = proofByHeight.get(height);
      ListProofHashedEntry present = proofsAtHeight.putIfAbsent(index, hashedEntry);
      if (present != null) {
        throw new InvalidProofException(
            String.format("Multiple proof entries at the same position: %s and %s",
                present, hashedEntry));
      }
    }
    return proofByHeight;
  }

  /**
   * Checks the element entries: no out-of-range elements; no duplicate indexes.
   */
  private void checkElementEntries() {
    Map<Long, ListProofElementEntry> elementsByIndex = newHashMapWithExpectedSize(elements.size());
    for (ListProofElementEntry e : elements) {
      long index = e.getIndex();
      if (index < 0L || size <= index) {
        throw new InvalidProofException(
            String.format("Entry at invalid index (%d), must be in range [0; %d): %s",
                index, size, e));
      }
      ListProofElementEntry present = elementsByIndex.putIfAbsent(index, e);
      if (present != null) {
        throw new InvalidProofException(
            String.format("Multiple element entries at the same index (%d): %s and %s", index,
                present, e));
      }
    }
  }

  /**
   * Computes the root hash of the proof list tree, also verifying the correctness of
   * proof entries with regard to the calculated ones.
   *
   * @param proofByHeight proof entries indexed by their height
   * @param treeHeight the height of the tree
   */
  private HashCode computeRootHash(List<Map<Long, ListProofHashedEntry>> proofByHeight,
      int treeHeight) {
    // Hash the element entries, and obtain the first level of calculated hashes
    Map<Long, ListProofHashedEntry> calculated = hashElements();

    // For each tree level, starting at the bottom
    for (int height = 0; height < treeHeight; height++) {
      // Take the proof (hashed) entries at this height
      Map<Long, ListProofHashedEntry> proofAtLevel = proofByHeight.get(height);
      // Merge the calculated entries with the proof entries at this height
      calculated = reduce(calculated, proofAtLevel);
    }

    // Take the root hash and calculate the index hash.
    return calculated.get(0L).getHash();
  }

  private Map<Long, ListProofHashedEntry> hashElements() {
    return elements.stream()
        .map(FlatListProof::hashLeafNode)
        .collect(toMap(ListProofEntry::getIndex, Functions.identity()));
  }

  /**
   * Combines the nodes at height h to produce their parent nodes at height h + 1.
   *
   * @param calculated the nodes inferred from the elements and the proof nodes from levels [0, h-1]
   * @param proofAtLevel the proof nodes at height h
   * @return the calculated nodes at height h + 1
   */
  private Map<Long, ListProofHashedEntry> reduce(Map<Long, ListProofHashedEntry> calculated,
      Map<Long, ListProofHashedEntry> proofAtLevel) {
    // Verify nodes:
    //  - For an inferred node n there is a sibling either in the inferred
    //  nodes or in hash nodes; or it is the last node in an odd-sized level.
    for (ListProofHashedEntry inferredNode : calculated.values()) {
      if (isLastOnOddLevel(inferredNode)) {
        // The last node on an odd-sized level does not have a sibling.
        continue;
      }
      long index = inferredNode.getIndex();
      long siblingIndex = getSiblingIndex(index);
      if (!(calculated.containsKey(siblingIndex) || proofAtLevel.containsKey(siblingIndex))) {
        throw new InvalidProofException(
            String.format("Missing proof entry at index (%d) for the calculated one: %s",
                siblingIndex, inferredNode));
      }
    }
    // Verify proof nodes:
    for (ListProofHashedEntry proofNode : proofAtLevel.values()) {
      long index = proofNode.getIndex();
      // No hash nodes overriding the inferred nodes (i.e., have same index)
      if (calculated.containsKey(index)) {
        throw new InvalidProofException(
            String.format("Redundant proof entry (%s) with the same index (%d) as "
                + "the calculated node (%s)", proofNode, index, calculated.get(index)));
      }
      // No redundant hash nodes that have no siblings in the inferred nodes.
      long siblingIndex = getSiblingIndex(index);
      if (!calculated.containsKey(siblingIndex)) {
        throw new InvalidProofException(
            String.format("Redundant proof entry (%s) not needed for verification", proofNode));
      }
    }

    // Merge calculated on the previous level hashes with the proof entries of the current
    // level, ordered by indexes. The sibling nodes will go in adjacent pairs.
    SortedMap<Long, ListProofHashedEntry> merged = new TreeMap<>();
    merged.putAll(calculated);
    merged.putAll(proofAtLevel);

    // Reduce the adjacent nodes to produce the calculated nodes on the upper level
    Iterator<ListProofHashedEntry> mergedIter = merged.values().iterator();
    Map<Long, ListProofHashedEntry> nextLevel = newHashMapWithExpectedSize((merged.size() + 1) / 2);
    while (mergedIter.hasNext()) {
      ListProofHashedEntry left = mergedIter.next();
      ListProofHashedEntry right = null;
      if (mergedIter.hasNext()) {
        right = mergedIter.next();
      }
      ListProofHashedEntry parent = hashBranchNode(left, right);
      nextLevel.put(parent.getIndex(), parent);
    }
    return nextLevel;
  }

  private long levelSizeAt(int height) {
    // Consider memoizing the level sizes to avoid re-calculation
    checkArgument(height >= 0);
    long levelSize = size;
    while (height-- != 0) {
      levelSize = (levelSize + 1) / 2;
    }
    return levelSize;
  }

  private boolean isLastOnOddLevel(ListProofHashedEntry inferredNode) {
    long levelSize = levelSizeAt(inferredNode.getHeight());
    long index = inferredNode.getIndex();
    return isOdd(levelSize) && (index == levelSize - 1);
  }

  private static long getSiblingIndex(long index) {
    if (isEven(index)) {
      return index + 1;
    } else {
      return index - 1;
    }
  }

  private static boolean isOdd(long v) {
    return !isEven(v);
  }

  private static boolean isEven(long v) {
    return (v & 1L) == 0L;
  }

  private static ListProofHashedEntry hashLeafNode(ListProofElementEntry elementEntry) {
    long index = elementEntry.getIndex();
    HashCode hash = newHasher()
        .putByte(BLOB_PREFIX)
        .putBytes(elementEntry.getElement())
        .hash();
    return ListProofHashedEntry.newInstance(index, 0, hash);
  }

  private static ListProofHashedEntry hashBranchNode(ListProofHashedEntry leftChild,
      @Nullable ListProofHashedEntry rightChild) {
    long index = leftChild.getIndex() / 2;
    int height = leftChild.getHeight() + 1;
    Hasher hasher = newHasher()
        .putByte(LIST_BRANCH_PREFIX)
        .putObject(leftChild.getHash(), hashCodeFunnel());
    if (rightChild != null) {
      hasher.putObject(rightChild.getHash(), hashCodeFunnel());
    }
    return ListProofHashedEntry.newInstance(index, height, hasher.hash());
  }

  private HashCode hashListIndex(HashCode rootHash) {
    return hashListIndex(size, rootHash);
  }

  private static HashCode hashListIndex(long size, HashCode rootHash) {
    return newHasher()
        .putByte(LIST_ROOT_PREFIX)
        .putLong(size)
        .putObject(rootHash, hashCodeFunnel())
        .hash();
  }

  private static Hasher newHasher() {
    return sha256().newHasher();
  }

  private CheckedListProofImpl<byte[]> newCheckedProof(HashCode indexHash) {
    NavigableMap<Long, byte[]> elements = indexElements();
    return new CheckedListProofImpl<>(size, indexHash, elements, ListProofStatus.VALID);
  }

  private NavigableMap<Long, byte[]> indexElements() {
    return elements.stream()
        .collect(toMap(ListProofEntry::getIndex, ListProofElementEntry::getElement,
            throwingMerger(), TreeMap::new));
  }

  private static <U> BinaryOperator<U> throwingMerger() {
    return (u, v) -> {
      throw new IllegalArgumentException("Duplicate values with the same key");
    };
  }
}
