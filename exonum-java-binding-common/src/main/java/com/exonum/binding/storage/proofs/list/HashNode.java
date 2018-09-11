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

package com.exonum.binding.storage.proofs.list;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.hash.HashCode;

/**
 * Represents a hash of a Merkle sub-tree: a leaf node in proof trees.
 */
public final class HashNode implements ListProof {

  private final HashCode hash;

  /**
   * Creates a new hash node.
   */
  @SuppressWarnings("unused")  // native API
  HashNode(byte[] hash) {
    this(HashCode.fromBytes(hash));
  }

  public HashNode(HashCode hash) {
    this.hash = checkNotNull(hash);
  }

  @Override
  public void accept(ListProofVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * Returns the hash value.
   */
  public HashCode getHash() {
    return hash;
  }
}
