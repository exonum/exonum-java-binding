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

import static com.exonum.binding.common.proofs.list.FlatListProof.calcTreeHeight;
import static com.exonum.binding.common.proofs.list.ListProofEntry.MAX_HEIGHT;
import static com.exonum.binding.common.proofs.list.ListProofEntry.MAX_INDEX;
import static com.exonum.binding.common.proofs.list.ListProofUtils.getBranchHashCode;
import static com.exonum.binding.common.proofs.list.ListProofUtils.getLeafHashCode;
import static com.exonum.binding.common.proofs.list.ListProofUtils.getProofListHash;
import static com.exonum.binding.test.Bytes.bytes;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.test.Bytes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/*
The test uses the following illustrations to aid understanding of the proof structure:

 H — height of nodes at each level
 2        o
        /   \
 1    o       o
     / \     / \
 0  e   e   e   h
 _________________
 Legend:
   e — element proof entry (ListProofElementEntry)
   h — hashed proof entry (ListProofHashedEntry)
   o — 'virtual' node — not present in the proof, inferred during verification.
       Shown mostly to communicate the tree structure of the proof.
   ? — absent node — a node that is expected to be present in a valid proof, but isn't
   x — redundant node — a node that shall not be present in a valid proof, but is
 */
class FlatListProofTest {

  private static final HashCode EMPTY_LIST_INDEX_HASH =
      HashCode.fromString("c6c0aa07f27493d2f2e5cff56c890a353a20086d6c25ec825128e12ae752b2d9");

  private static final List<byte[]> ELEMENTS = createElements(8);
  private static final List<ListProofElementEntry> ELEMENT_ENTRIES = createElementEntries(ELEMENTS);

  @ParameterizedTest
  @MethodSource("calcTreeHeightSource")
  void testCalcTreeHeight(long size, int expectedHeight) {
    assertThat(calcTreeHeight(size)).isEqualTo(expectedHeight);
  }

  private static List<Arguments> calcTreeHeightSource() {
    int maxHeight = 56;
    long maxSize = 0x100_0000_0000_0000L; // 2^56
    return asList(
        //       | size | height |
        arguments(0, 0),
        arguments(1, 0),
        arguments(2, 1),
        arguments(3, 2),
        arguments(4, 2),
        arguments(5, 3),
        arguments(8, 3),
        arguments(9, 4),
        arguments(maxSize / 2 - 1, maxHeight - 1),
        arguments(maxSize / 2, maxHeight - 1),
        arguments(maxSize / 2 + 1, maxHeight),
        arguments(maxSize - 1, maxHeight),
        arguments(maxSize, maxHeight)
    );
  }

  @Test
  void emptyListValidProof() {
    FlatListProof proof = new FlatListProof(emptyList(), emptyList(), 0L);

    CheckedListProof<byte[]> checked = proof.verify();

    assertTrue(checked.isValid());
    assertThat(checked.size()).isZero();
    assertThat(checked.getIndexHash()).isEqualTo(EMPTY_LIST_INDEX_HASH);
  }

  @Test
  void emptyListInvalidProofIfHasElements() {
    ListProofElementEntry unexpectedElement = ELEMENT_ENTRIES.get(0);
    FlatListProof proof = new FlatListProof(singletonList(unexpectedElement), emptyList(), 0L);

    InvalidProofException e = assertThrows(InvalidProofException.class, proof::verify);

    assertThat(e).hasMessageContaining("must not have elements")
        .hasMessageContaining(unexpectedElement.toString());
  }

  @Test
  void emptyListInvalidProofIfHasHashedEntries() {
    ListProofHashedEntry unexpectedEntry = hashedEntry(0, 0, HashCode.fromInt(1));
    FlatListProof proof = new FlatListProof(emptyList(), singletonList(unexpectedEntry), 0L);

    InvalidProofException e = assertThrows(InvalidProofException.class, proof::verify);

    assertThat(e).hasMessageContaining("Proof for empty list must not have proof entries")
        .hasMessageContaining(unexpectedEntry.toString());
  }

  @Test
  void singletonListValidProof() {
    int index = 0;
    ListProofElementEntry element = ELEMENT_ENTRIES.get(index);
    long size = 1L;
    FlatListProof proof = new FlatListProof(singletonList(element), emptyList(), size);

    CheckedListProof<byte[]> checked = proof.verify();

    assertTrue(checked.isValid());
    assertThat(checked.size()).isEqualTo(size);
    byte[] e0Value = ELEMENTS.get(index);
    assertThat(checked.getElements()).containsExactly(entry((long) index, e0Value));
    HashCode rootHash = getLeafHashCode(e0Value);
    HashCode expectedListHash = getProofListHash(rootHash, size);
    assertThat(checked.getIndexHash()).isEqualTo(expectedListHash);
  }

  // todo: more singletonListInvalid

  @ParameterizedTest
  @CsvSource({
      // size of the list, height of the tree (and the root node)
      "1, 0",
      "2, 1",
      "3, 2",
      "4, 2",
      "5, 3",
      "8, 3"
  })
  void emptyRangeValidProof(long size, int height) {
    /*
     H
     height        h — a single hash entry for the root, no elements
     */
    ListProofHashedEntry h1 = hashedEntry(0L, height);
    FlatListProof proof = new FlatListProofBuilder()
        .size(size)
        .addProofEntry(h1)
        .build();

    CheckedListProof<byte[]> checked = proof.verify();

    assertTrue(checked.isValid());
    assertThat(checked.size()).isEqualTo(size);
    assertThat(checked.getElements()).isEmpty();

    HashCode rootHash = h1.getHash();
    HashCode expectedListHash = getProofListHash(rootHash, size);
    assertThat(checked.getIndexHash()).isEqualTo(expectedListHash);
  }

  @ParameterizedTest
  @ValueSource(longs = {1, 2, 3, 4})
  void emptyRangeInvalidProofNoHash(long size) {
    FlatListProof proof = new FlatListProofBuilder()
        .size(size)
        .build();

    InvalidProofException e = assertThrows(InvalidProofException.class,
        proof::verify);
    assertThat(e).hasMessageContaining("Proof for an empty range must have a single proof node");
  }

  @ParameterizedTest
  @CsvSource({
      // index | height. The only correct pair is 0, 2 for a 4-element list.
      // Invalid indices
      "1, 2",
      "2, 2",
      // Invalid heights
      //   Below
      "0, 0",
      "0, 1",
      //   Above
      "0, 3",
      "0, 4"
  })
  void emptyRangeInvalidProofBadRootHashEntry(long index, int height) {
    /*
    H
    2     ? — root hashed entry at position (0, 2)

    1    {x} — various hashed entries scattered among levels

    0
     */
    ListProofHashedEntry h1 = hashedEntry(index, height);
    FlatListProof proof = new FlatListProofBuilder()
        .size(4L)
        .addProofEntry(h1)
        .build();

    InvalidProofException e = assertThrows(InvalidProofException.class,
        proof::verify);
    assertThat(e).hasMessageContaining(h1.toString());
  }

  @ParameterizedTest
  @MethodSource("emptyRangeInvalidProofRedundantHashNodesSource")
  void emptyRangeInvalidProofRedundantHashNodes(List<ListProofHashedEntry> redundantEntries) {
    /*
    H
    2     h — root hashed entry

    1    {x} — various hashed entries scattered among levels,
               with valid and invalid indexes

    0
     */
    ListProofHashedEntry rootHashEntry = hashedEntry(0L, 2);
    FlatListProof proof = new FlatListProofBuilder()
        .size(4L)
        .addProofEntry(rootHashEntry)
        .addProofEntries(redundantEntries)
        .build();
    InvalidProofException e = assertThrows(InvalidProofException.class,
        proof::verify);
    assertThat(e).hasMessageContaining("Proof for an empty range must have a single proof node")
        .hasMessageContaining(String.valueOf(redundantEntries.size() + 1));
  }

  static List<List<ListProofHashedEntry>> emptyRangeInvalidProofRedundantHashNodesSource() {
    return asList(
        singletonList(hashedEntry(0L, 1)),
        // Sibling nodes at height 1, with valid indices
        asList(hashedEntry(0L, 1), hashedEntry(1L, 1)),
        // Nodes with invalid heights/indexes
        // Invalid heights
        singletonList(hashedEntry(0L, 3)),
        // Invalid indices
        singletonList(hashedEntry(2L, 1))
    );
  }

  @Test
  void twoElementListValidProofE0() {
    /*
     H
     1        o
            /   \
     0    e       h
     */
    int index = 0;
    long size = 2L;
    ListProofHashedEntry h1 = hashedEntry(1, 0);
    FlatListProof proof = new FlatListProofBuilder()
        .size(size)
        .addElement(ELEMENT_ENTRIES.get(index))
        .addProofEntry(h1)
        .build();

    CheckedListProof<byte[]> checked = proof.verify();

    assertTrue(checked.isValid());
    assertThat(checked.size()).isEqualTo(size);
    byte[] e0value = ELEMENTS.get(index);
    assertThat(checked.getElements()).containsExactly(entry((long) index, e0value));

    HashCode node0Hash = getLeafHashCode(e0value);
    HashCode node1Hash = h1.getHash();
    HashCode rootHash = getBranchHashCode(node0Hash, node1Hash);
    HashCode expectedListHash = getProofListHash(rootHash, size);
    assertThat(checked.getIndexHash()).isEqualTo(expectedListHash);
  }

  @Test
  void twoElementListValidProofE1() {
    /*
     H
     1        o
            /   \
     0    h       e
     */
    int index = 1;
    long size = 2L;
    ListProofHashedEntry h0 = hashedEntry(0, 0);
    FlatListProof proof = new FlatListProofBuilder()
        .size(size)
        .addElement(ELEMENT_ENTRIES.get(index))
        .addProofEntry(h0)
        .build();

    CheckedListProof<byte[]> checked = proof.verify();

    assertTrue(checked.isValid());
    assertThat(checked.size()).isEqualTo(size);
    byte[] e1value = ELEMENTS.get(index);
    assertThat(checked.getElements()).containsExactly(entry((long) index, e1value));

    HashCode node0Hash = h0.getHash();
    HashCode node1Hash = getLeafHashCode(e1value);
    HashCode rootHash = getBranchHashCode(node0Hash, node1Hash);
    HashCode expectedListHash = getProofListHash(rootHash, size);
    assertThat(checked.getIndexHash()).isEqualTo(expectedListHash);
  }

  @Test
  void twoElementListValidProofFullProof() {
    /*
     H
     1        o
            /   \
     0    e       e
     */
    long size = 2L;
    FlatListProof proof = new FlatListProofBuilder()
        .size(size)
        .addElements(ELEMENT_ENTRIES.subList(0, (int) size))
        .build();

    CheckedListProof<byte[]> checked = proof.verify();

    assertTrue(checked.isValid());
    assertThat(checked.size()).isEqualTo(size);
    assertThat(checked.getElements()).containsExactly(
        entry(0L, ELEMENTS.get(0)),
        entry(1L, ELEMENTS.get(1)));

    HashCode node0Hash = getLeafHashCode(ELEMENTS.get(0));
    HashCode node1Hash = getLeafHashCode(ELEMENTS.get(1));
    HashCode rootHash = getBranchHashCode(node0Hash, node1Hash);
    HashCode expectedListHash = getProofListHash(rootHash, size);
    assertThat(checked.getIndexHash()).isEqualTo(expectedListHash);
  }

  @Test
  void twoElementListInvalidProofMissingHashNodeAt1() {
    /*
     H
     1        o
            /   \
     0    e       ?
     */
    int index = 0;
    ListProofElementEntry element = ELEMENT_ENTRIES.get(index);
    long size = 2L;
    FlatListProof proof = new FlatListProof(singletonList(element), emptyList(), size);

    InvalidProofException e = assertThrows(InvalidProofException.class,
        proof::verify);

    assertThat(e).hasMessageContaining("Missing proof entry at index (1)");
  }

  @Test
  void twoElementListInvalidProofMissingHashNodeAt0() {
    /*
     H
     1        o
            /   \
     0    ?       e
     */
    int index = 1;
    ListProofElementEntry element = ELEMENT_ENTRIES.get(index);
    long size = 2L;
    FlatListProof proof = new FlatListProof(singletonList(element), emptyList(), size);

    InvalidProofException e = assertThrows(InvalidProofException.class,
        proof::verify);

    assertThat(e).hasMessageContaining("Missing proof entry at index (0)");
  }

  @ParameterizedTest
  @MethodSource("twoElementListInvalidProofNodesAboveMaxHeightSource")
  void twoElementListInvalidProofNodesAboveMaxHeight(List<ListProofHashedEntry> invalidEntries) {
    /*
     H
     k       {x} <— invalid nodes at various heights k > 1

     1        o
            /   \
     0    e       h
     */
    FlatListProof proof = twoElementListAt0()
        .addProofEntries(invalidEntries)
        .build();

    InvalidProofException e = assertThrows(InvalidProofException.class,
        proof::verify);

    assertThat(e).hasMessageContaining("Proof entry at invalid height")
        .hasMessageContaining("must be in range [0; 1)");
  }

  static List<List<ListProofHashedEntry>> twoElementListInvalidProofNodesAboveMaxHeightSource() {
    return asList(
        //                        | index | height |
        singletonList(hashedEntry(0, 1)),
        singletonList(hashedEntry(0, 2)),
        singletonList(hashedEntry(1, 2)),
        singletonList(hashedEntry(0, 3)),
        singletonList(hashedEntry(0, MAX_HEIGHT)),
        asList(hashedEntry(0, 2), hashedEntry(1, 2))
    );
  }

  @ParameterizedTest
  @MethodSource("twoElementListInvalidProofExtraNodesAtInvalidIndexesSource")
  void twoElementListInvalidProofExtraNodesAtInvalidIndexes(
      List<ListProofEntry> invalidProofEntries) {
    /*
     H
     1        o       {x} <- invalid nodes at various indexes exceeding max
            /   \
     0    e       h   {x} <—
     */
    FlatListProof proof = twoElementListAt0()
        .addAnyEntries(invalidProofEntries)
        .build();

    InvalidProofException e = assertThrows(InvalidProofException.class,
        proof::verify);

    String message = e.getMessage();
    assertThat(message)
        .containsIgnoringCase("entry at invalid index")
        .contains("must be in range [0; ");
  }

  private static Collection<List<ListProofEntry>>
      twoElementListInvalidProofExtraNodesAtInvalidIndexesSource() {
    /*
    Target proof tree:
     H
     1        o
            /   \
     0    e       h
     */
    byte[] value = bytes(1, 2, 3);
    return asList(
        //                        | index | height |
        singletonList(hashedEntry(2, 0)),
        singletonList(hashedEntry(3, 0)),
        singletonList(elementEntry(2, value)),
        singletonList(elementEntry(3, value)),
        singletonList(elementEntry(MAX_INDEX, value)),
        singletonList(hashedEntry(ListProofEntry.MAX_INDEX, 0)),
        asList(hashedEntry(2, 0), hashedEntry(3, 0))
    );
  }

  @Test
  void twoElementListInvalidProofExtraNodesRedundantCalculated() {
    /*
     H
     1        o
           / /  \
     0    e x    h
     */
    ListProofHashedEntry redundantNode = hashedEntry(0L, 0);
    FlatListProof proof = twoElementListAt0()
        .addProofEntry(redundantNode)
        .build();

    InvalidProofException e = assertThrows(InvalidProofException.class,
        proof::verify);

    assertThat(e).hasMessageContaining("Redundant proof entry")
        .hasMessageContaining(redundantNode.toString())
        .hasMessageFindingMatch("with the same index .+ as the calculated node");
  }

  @Test
  void twoElementListInvalidProofExtraNodesDuplicateHashed() {
    /*
     H
     1        o
           /    \ \
     0    e      h h
     */
    ListProofHashedEntry duplicateNode = hashedEntry(1, 0);
    FlatListProof proof = twoElementListAt0()
        .addProofEntry(duplicateNode)
        .build();

    InvalidProofException e = assertThrows(InvalidProofException.class,
        proof::verify);

    assertThat(e).hasMessageContaining("Multiple proof entries at the same position")
        .hasMessageContaining(duplicateNode.toString());
  }

  @Test
  void twoElementListInvalidProofExtraNodesDuplicateElementSameValue() {
    /*
     H
     1        o
           / /  \
     0    e e    h
     */
    ListProofElementEntry duplicateEntry = ELEMENT_ENTRIES.get(0);
    FlatListProof proof = twoElementListAt0()
        .addElement(duplicateEntry)
        .build();

    InvalidProofException e = assertThrows(InvalidProofException.class,
        proof::verify);

    assertThat(e.getMessage())
        .containsIgnoringCase("multiple element entries at the same index (0)")
        .contains(duplicateEntry.toString());
  }

  @Test
  void twoElementListInvalidProofExtraNodesDuplicateElementDiffValue() {
    /*
     H
     1        o
           / /  \
     0    e e    h
     */
    ListProofElementEntry entry = ELEMENT_ENTRIES.get(0);
    ListProofElementEntry conflictingEntry = elementEntry(0, bytes(1, 2, 3, 4));
    FlatListProof proof = twoElementListAt0()
        .addElement(conflictingEntry)
        .build();

    InvalidProofException e = assertThrows(InvalidProofException.class,
        proof::verify);

    assertThat(e.getMessage())
        .containsIgnoringCase("multiple element entries at the same index (0)")
        .contains(entry.toString())
        .contains(conflictingEntry.toString());
  }

  @Test
  void twoElementListValidProofAt0() {
    // Check the proof returned by twoElementListAt0 is valid
    FlatListProof proof = twoElementListAt0().build();
    CheckedListProof<byte[]> checked = proof.verify();
    assertTrue(checked.isValid());
  }

  /**
   * Returns a builder with a structurally valid flat proof for a tree of size 2
   * and an element at index 0.
   */
  private FlatListProofBuilder twoElementListAt0() {
    /*
     H
     1        o
            /   \
     0    e       h
     */
    long size = 2L;
    ListProofElementEntry e0 = ELEMENT_ENTRIES.get(0);
    ListProofHashedEntry h1 = hashedEntry(1, 0);
    return new FlatListProofBuilder()
        .size(size)
        .addElement(e0)
        .addProofEntry(h1);
  }

  @Test
  void threeElementListValidProofE2() {
    /*
     H — height of nodes at each level
     2        o
            /   \
     1    h       o
                 /
     0          e
     */
    int index = 2;
    ListProofElementEntry elementEntry = ELEMENT_ENTRIES.get(index);
    long size = 3;
    ListProofHashedEntry h0 = hashedEntry(0, 1);
    FlatListProof proof = new FlatListProofBuilder()
        .size(size)
        .addElement(elementEntry)
        .addProofEntry(h0)
        .build();

    CheckedListProof<byte[]> checked = proof.verify();

    assertThat(checked.size()).isEqualTo(size);
    assertThat(checked.getElements())
        .containsExactly(entry((long) index, ELEMENTS.get(index)));

    // Check the hash code
    HashCode leafHash = getLeafHashCode(ELEMENTS.get(index));
    HashCode node0Hash = h0.getHash();
    HashCode node1Hash = getBranchHashCode(leafHash, null);
    HashCode rootHash = getBranchHashCode(node0Hash, node1Hash);
    HashCode expectedListHash = getProofListHash(rootHash, size);
    assertThat(checked.getIndexHash()).isEqualTo(expectedListHash);
  }

  @Test
  void fourElementListInvalidProofMissingHashNode1() {
    /*
     H
     2        o
            /   \
     1    o       ?
         / \
     0  e   h
     */
    FlatListProof proof = new FlatListProofBuilder()
        .size(4)
        .addElement(ELEMENT_ENTRIES.get(0))
        .addProofEntry(hashedEntry(1, 0))
        .build();

    InvalidProofException e = assertThrows(InvalidProofException.class,
        proof::verify);

    assertThat(e).hasMessageContaining("Missing proof entry at index (1)");
  }

  @Test
  void fourElementListInvalidProofMissingHashNode2() {
    /*
     H — height of nodes at each level
     2        o
            /   \
     1    ?       o
                 / \
     0          e   h
     */
    FlatListProof proof = new FlatListProofBuilder()
        .size(4)
        .addElement(ELEMENT_ENTRIES.get(2))
        .addProofEntry(hashedEntry(3, 0))
        .build();

    InvalidProofException e = assertThrows(InvalidProofException.class,
        proof::verify);

    assertThat(e).hasMessageContaining("Missing proof entry at index (0)");
  }

  @Test
  void fourElementListInvalidProofExtraNodesRedundantTotally() {
    /*
     H
     2        o
            /   \
     1    o       h
         / \     / \
     0  e   h   x   x — the last two hashes are totally redundant as their parent must be
                        and is present in the proof tree
     */
    ListProofHashedEntry r1 = hashedEntry(2, 0);
    ListProofHashedEntry r2 = hashedEntry(3, 0);
    FlatListProof proof = new FlatListProofBuilder()
        .size(4)
        .addElement(ELEMENT_ENTRIES.get(0))
        .addProofEntries(hashedEntry(1, 0), hashedEntry(1, 1))
        // Totally redundant hashed entries at height 0
        .addProofEntries(r1, r2)
        .build();

    InvalidProofException e = assertThrows(InvalidProofException.class,
        proof::verify);

    assertThat(e).hasMessageContaining("Redundant proof entry")
        .hasMessageContaining("not needed for verification");
    assertThat(e).satisfiesAnyOf(
        e1 -> assertThat(e1).hasMessageContaining(r1.toString()),
        e1 -> assertThat(e1).hasMessageContaining(r2.toString())
    );
  }

  @ParameterizedTest
  @CsvSource({
      // Overriding the calculated hashes of element entries (h=0)
      "0, 0",
      "1, 0",
      "4, 0",
      // Overriding the calculated hashes at intermediate levels
      "0, 1",
      "2, 1",
      "0, 2",
      "1, 2",
  })
  void fiveElementListInvalidProofExtraNodesRedundantCalculated(long index, int height) {
    /*
    Target proof tree of size 5 with two ranges [0, 1], [4]:
     H
     3          o
              /   \
     2      o       o
           / \     /
     1    o   h   o
         /\      /
     0  e e     e
     */
    ListProofHashedEntry redundantNode = hashedEntry(index, height);
    FlatListProof proof = new FlatListProofBuilder()
        .size(5L)
        .addElements(ELEMENT_ENTRIES.get(0), ELEMENT_ENTRIES.get(1), ELEMENT_ENTRIES.get(4))
        .addProofEntry(hashedEntry(1L, 1))
        .addProofEntry(redundantNode)
        .build();

    InvalidProofException e = assertThrows(InvalidProofException.class,
        proof::verify);

    assertThat(e.getMessage()).containsIgnoringCase("Redundant proof entry")
        .contains(redundantNode.toString())
        .contains(String.format("with the same index (%d) as the calculated node", index));
  }

  @ParameterizedTest
  @MethodSource("fiveElementListInvalidProofExtraNodesAtInvalidIndexesSource")
  void fiveElementListInvalidProofExtraNodesAtInvalidIndexes(
      List<ListProofEntry> invalidProofEntries) {
    /*
     H
     3          o
              /   \
     2      h       o
                   /
     1            o  {x} <- invalid nodes at various heights with indexes exceeding max
                 /
     0          e    {x} <—
     */
    FlatListProof proof = fiveElementListAt4()
        .addAnyEntries(invalidProofEntries)
        .build();

    InvalidProofException e = assertThrows(InvalidProofException.class,
        proof::verify);

    assertThat(e.getMessage()).containsIgnoringCase("entry at invalid index");
  }

  private static Collection<List<ListProofEntry>>
      fiveElementListInvalidProofExtraNodesAtInvalidIndexesSource() {
    /*
    Target proof tree:
     H
     3          o
              /   \
     2      h       o
                   /
     1            o  {x} <- invalid nodes at various heights with indexes exceeding max
                 /
     0          e    {x} <—
     */
    byte[] value = bytes(1, 2, 3);
    return asList(
        //                        | index | height |
        // At height 0
        singletonList(hashedEntry(5, 0)),
        singletonList(hashedEntry(6, 0)),
        singletonList(hashedEntry(ListProofEntry.MAX_INDEX, 0)),
        singletonList(elementEntry(5, value)),
        singletonList(elementEntry(6, value)),
        singletonList(elementEntry(MAX_INDEX, value)),
        asList(hashedEntry(5, 0), hashedEntry(7, 0)),
        // At height 1
        singletonList(hashedEntry(3, 1)),
        singletonList(hashedEntry(4, 1))
    );
  }

  @Test
  void fiveElementListValidProofAt4() {
    // Check the proof returned by fiveElementListAt4 is valid
    FlatListProof proof = fiveElementListAt4().build();
    CheckedListProof<byte[]> checked = proof.verify();
    assertTrue(checked.isValid());
    assertThat(checked.getElements()).containsExactly(entry(4L, ELEMENTS.get(4)));
  }

  private FlatListProofBuilder fiveElementListAt4() {
    /*
     H
     3          o
              /   \
     2      h       o
                   /
     1            o
                 /
     0          e
     */
    return new FlatListProofBuilder()
        .size(5)
        .addElement(ELEMENT_ENTRIES.get(4))
        .addProofEntry(hashedEntry(0, 2));
  }

  @ParameterizedTest
  @MethodSource("eightElementListInvalidProofExtraNodesRedundantTotallySource")
  void eightElementListInvalidProofExtraNodesRedundantTotally(
      List<ListProofHashedEntry> invalidEntries
  ) {
    /*
     H
     3          o
              /   \
     2      o       h
           / \
     1    o   h
         /\
     0   e h
     */
    FlatListProof proof = new FlatListProofBuilder()
        .size(8)
        .addElement(ELEMENT_ENTRIES.get(0))
        .addProofEntries(hashedEntry(1, 0), hashedEntry(1, 1), hashedEntry(1, 2))
        // Totally redundant hashed entries at height 0
        .addProofEntries(invalidEntries)
        .build();

    InvalidProofException e = assertThrows(InvalidProofException.class,
        proof::verify);

    assertThat(e).hasMessageContaining("Redundant proof entry")
      .hasMessageContaining("not needed for verification");
  }

  static Collection<List<ListProofHashedEntry>>
      eightElementListInvalidProofExtraNodesRedundantTotallySource() {
    /*
     H
     2          o
              /   \
     1      o       h
           / \
     0    o   h   1   2
         /\
     0   e h 2 3 4 5 6 7

     index is used instead of 'x' to show all possible redundant hashed entries
     */
    return asList(
        // At height 0
        singletonList(hashedEntry(2, 0)),
        singletonList(hashedEntry(3, 0)),
        singletonList(hashedEntry(7, 0)),
        asList(hashedEntry(6,0), hashedEntry(7, 0)),
        // At height 1
        singletonList(hashedEntry(2, 1)),
        singletonList(hashedEntry(3, 1)),
        asList(hashedEntry(2, 1), hashedEntry(3, 1))
    );
  }

  /**
   * Creates a given number of single-byte elements, with their first and only byte
   * equal to the index they are stored at.
   */
  private static List<byte[]> createElements(int size) {
    return IntStream.range(0, size)
        .mapToObj(Bytes::bytes)
        .collect(toList());
  }

  /**
   * Creates list proof entries for each element of the given list.
   * @param list a list of elements
   */
  private static List<ListProofElementEntry> createElementEntries(List<byte[]> list) {
    int size = list.size();
    return IntStream.range(0, size)
        .mapToObj(i -> elementEntry(i, list.get(i)))
        .collect(toList());
  }

  private static ListProofElementEntry elementEntry(long index, byte[] element) {
    return ListProofElementEntry.newInstance(index, element);
  }

  private static ListProofHashedEntry hashedEntry(long index, int height) {
    HashCode nodeHash = Hashing.sha256().newHasher()
        .putLong(index)
        .putInt(height)
        .hash();
    return ListProofHashedEntry.newInstance(index, height, nodeHash);
  }

  private static ListProofHashedEntry hashedEntry(long index, int height, HashCode nodeHash) {
    return ListProofHashedEntry.newInstance(index, height, nodeHash);
  }

  private static class FlatListProofBuilder {
    List<ListProofElementEntry> elements = new ArrayList<>();
    List<ListProofHashedEntry> proof = new ArrayList<>();
    long size = 0L;

    FlatListProofBuilder size(long size) {
      this.size = size;
      return this;
    }

    FlatListProofBuilder addElement(ListProofElementEntry elementEntry) {
      elements.add(elementEntry);
      return this;
    }

    FlatListProofBuilder addElements(ListProofElementEntry... elementEntries) {
      return addElements(asList(elementEntries));
    }

    FlatListProofBuilder addElements(List<ListProofElementEntry> elementEntries) {
      elements.addAll(elementEntries);
      return this;
    }

    FlatListProofBuilder addProofEntry(ListProofHashedEntry h1) {
      proof.add(h1);
      return this;
    }

    FlatListProofBuilder addProofEntries(ListProofHashedEntry... proofHashedEntries) {
      return addProofEntries(asList(proofHashedEntries));
    }

    FlatListProofBuilder addProofEntries(List<ListProofHashedEntry> proofHashedEntries) {
      proof.addAll(proofHashedEntries);
      return this;
    }

    FlatListProofBuilder addAnyEntries(List</* intentionally no wildcards to limit its use */
        ListProofEntry> entries) {
      for (ListProofEntry e : entries) {
        if (e instanceof ListProofHashedEntry) {
          addProofEntry((ListProofHashedEntry) e);
        } else if (e instanceof ListProofElementEntry) {
          addElement((ListProofElementEntry) e);
        } else {
          throw new AssertionError("" + e);
        }
      }
      return this;
    }

    FlatListProof build() {
      return new FlatListProof(elements, proof, size);
    }
  }
}
