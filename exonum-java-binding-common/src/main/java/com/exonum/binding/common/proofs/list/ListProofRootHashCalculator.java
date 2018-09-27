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

package com.exonum.binding.common.proofs.list;

import static com.exonum.binding.common.hash.Funnels.hashCodeFunnel;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.HashFunction;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.hash.PrimitiveSink;
import com.exonum.binding.common.serialization.CheckingSerializerDecorator;
import com.exonum.binding.common.serialization.Serializer;
import com.google.common.annotations.VisibleForTesting;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 * List proof root hash calculator.
 *
 * @param <E> the type of elements in the corresponding list
 */
public final class ListProofRootHashCalculator<E> implements ListProofVisitor {

  private final CheckingSerializerDecorator<E> serializer;

  private final HashFunction hashFunction;

  private final NavigableMap<Long, E> elements;

  private long index;

  private HashCode calculatedRootHash;

  /**
   * Creates a new ListProofRootHashCalculator.
   *
   * @param serializer a serializer of list elements
   */
  public ListProofRootHashCalculator(Serializer<E> serializer) {
    this(serializer, Hashing.defaultHashFunction());
  }

  // to easily inject a mock of a calculatedRootHash function.
  @VisibleForTesting
  ListProofRootHashCalculator(Serializer<E> serializer, HashFunction hashFunction) {
    this.serializer = CheckingSerializerDecorator.from(serializer);
    this.hashFunction = checkNotNull(hashFunction);
    elements = new TreeMap<>();
    index = 0;
    calculatedRootHash = null;
  }

  @Override
  public void visit(ListProofBranch branch) {
    long branchIndex = index;
    HashCode leftHash = visitLeft(branch, branchIndex);
    Optional<HashCode> rightHash = visitRight(branch, branchIndex);

    calculatedRootHash = hashFunction.newHasher()
        .putObject(leftHash, hashCodeFunnel())
        .putObject(rightHash, (Optional<HashCode> from, PrimitiveSink into) ->
            from.ifPresent((hash) -> hashCodeFunnel().funnel(hash, into)))
        .hash();
  }

  @Override
  public void visit(ListProofHashNode listProofHashNode) {
    this.calculatedRootHash = listProofHashNode.getHash();
  }

  @Override
  public void visit(ListProofElement value) {
    assert !elements.containsKey(index) :
        "Error: already an element by such index in the map: i=" + index
            + ", e=" + elements.get(index);

    E element = serializer.fromBytes(value.getElement());
    elements.put(index, element);
    calculatedRootHash = hashFunction.hashObject(value, ListProofElement.funnel());
  }

  private HashCode visitLeft(ListProofBranch branch, long branchIndex) {
    index = 2 * branchIndex;
    branch.getLeft().accept(this);
    return calculatedRootHash;
  }

  private Optional<HashCode> visitRight(ListProofBranch branch, long branchIndex) {
    index = 2 * branchIndex + 1;
    calculatedRootHash = null;
    branch.getRight().ifPresent((right) -> right.accept(this));
    return Optional.ofNullable(calculatedRootHash);
  }

  /**
   * Returns a non-empty collection of list entries: index-element pairs, ordered by indices.
   *
   * @throws IllegalStateException if proof is not valid
   */
  public NavigableMap<Long, E> getElements() {
    return elements;
  }

  /**
   * Returns calculated root hash of a list proof tree.
   *
   * @return hash code
   */
  public HashCode getCalculatedRootHash() {
    return calculatedRootHash;
  }

  @Override
  public String toString() {
    return "ListProofStructureValidator{"
        + "calculatedRootHash=" + calculatedRootHash
        + ", elements=" + elements
        + ", index=" + index
        + '}';
  }
}
