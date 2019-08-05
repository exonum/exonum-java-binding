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
 * List proof hash calculator.
 *
 * @param <E> the type of elements in the corresponding list
 */
final class ListProofHashCalculator<E> implements ListProofVisitor {

  @VisibleForTesting
  static final byte BLOB_PREFIX = 0x00;
  @VisibleForTesting
  static final byte LIST_BRANCH_PREFIX = 0x01;
  @VisibleForTesting
  static final byte LIST_ROOT_PREFIX = 0x02;

  private final CheckingSerializerDecorator<E> serializer;

  private final HashFunction hashFunction;

  private final NavigableMap<Long, E> elements;

  private long index;

  private HashCode hash;

  /**
   * Creates a new ListProofHashCalculator.
   *
   * @param serializer a serializer of list elements
   */
  ListProofHashCalculator(ListProof listProof, Serializer<E> serializer) {
    this(listProof, serializer, Hashing.defaultHashFunction());
  }

  private ListProofHashCalculator(ListProof listProof, Serializer<E> serializer,
                                  HashFunction hashFunction) {
    this.serializer = CheckingSerializerDecorator.from(serializer);
    this.hashFunction = checkNotNull(hashFunction);
    elements = new TreeMap<>();
    index = 0;
    hash = null;

    ListProofNode rootNode = listProof.getRootNode();
    rootNode.accept(this);

    hash = hashFunction.newHasher()
        .putByte(LIST_ROOT_PREFIX)
        .putLong(listProof.getLength())
        .putObject(hash, hashCodeFunnel())
        .hash();
  }

  @Override
  public void visit(ListProofBranch branch) {
    long branchIndex = index;
    HashCode leftHash = visitLeft(branch, branchIndex);
    Optional<HashCode> rightHash = visitRight(branch, branchIndex);

    hash = hashFunction.newHasher()
        .putByte(LIST_BRANCH_PREFIX)
        .putObject(leftHash, hashCodeFunnel())
        .putObject(rightHash, (Optional<HashCode> from, PrimitiveSink into) ->
            from.ifPresent((hash) -> hashCodeFunnel().funnel(hash, into)))
        .hash();
  }

  @Override
  public void visit(ListProofHashNode listProofHashNode) {
    this.hash = listProofHashNode.getHash();
  }

  @Override
  public void visit(ListProofElement value) {
    assert !elements.containsKey(index) :
        "Error: already an element by such index in the map: i=" + index
            + ", e=" + elements.get(index);

    E element = serializer.fromBytes(value.getElement().toByteArray());
    elements.put(index, element);
    hash = hashFunction.newHasher()
        .putByte(BLOB_PREFIX)
        .putObject(value, ListProofElement.funnel())
        .hash();
  }

  private HashCode visitLeft(ListProofBranch branch, long parentIndex) {
    index = 2 * parentIndex;
    branch.getLeft().accept(this);
    return hash;
  }

  private Optional<HashCode> visitRight(ListProofBranch branch, long parentIndex) {
    index = 2 * parentIndex + 1;
    hash = null;
    branch.getRight().ifPresent((right) -> right.accept(this));
    return Optional.ofNullable(hash);
  }

  /**
   * Returns a collection of list entries: index-element pairs, ordered by indices.
   */
  NavigableMap<Long, E> getElements() {
    return elements;
  }

  /**
   * Returns calculated hash of a list proof tree.
   */
  HashCode getHash() {
    return hash;
  }

  @Override
  public String toString() {
    return "ListProofHashCalculator{"
        + "hash=" + hash
        + ", elements=" + elements
        + ", index=" + index
        + '}';
  }
}
