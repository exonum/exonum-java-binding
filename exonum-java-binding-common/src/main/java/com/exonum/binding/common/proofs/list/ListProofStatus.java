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

import com.exonum.binding.common.proofs.common.ProofStatus;

/**
 * Possible statuses of a checked list proof.
 */
public enum ListProofStatus implements ProofStatus {
  VALID("Proof has a valid structure"),
  INVALID_ELEMENT_NODE_DEPTH("Element node appears below the maximum allowed depth"),
  INVALID_HASH_NODE_DEPTH("Hash node appears below the maximum allowed depth"),
  INVALID_TREE_NO_ELEMENTS("Tree does not contain any element nodes"),
  INVALID_NODE_DEPTH("Value node appears at the wrong level"),
  INVALID_HASH_NODES_COUNT("Tree branch left and right node contains Hash nodes");

  private final String description;

  ListProofStatus(String description) {
    this.description = description;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public String toString() {
    return "MapProofStatus{"
        + "description='" + description + '\''
        + '}';
  }
}
