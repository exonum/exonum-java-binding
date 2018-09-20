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

package com.exonum.binding.common.proofs.model;

public interface ProofStatus {

  String getDescription();

  /**
   * Possible statuses of a checked list proof.
   */
  enum ListProofStatus implements ProofStatus {
    CORRECT("Proof has a valid structure"),
    INVALID("Proof has invalid structure");

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
      return "ProofStatus{"
          + "description='" + description + '\''
          + '}';
    }
  }

  /**
   * Possible statuses of a checked map proof.
   */
  enum MapProofStatus implements ProofStatus {
    CORRECT("Proof has a valid structure"),
    NON_TERMINAL_NODE("Proof entry in a singleton proof is of branch type (must be a leaf)"),
    INVALID_ORDER("Proof entries are placed in the wrong order"),
    DUPLICATE_PATH("There are entries with duplicate keys"),
    EMBEDDED_PATH("One key in the proof is a prefix of another key");

    private final String description;

    MapProofStatus(String description) {
      this.description = description;
    }

    @Override
    public String getDescription() {
      return this.description;
    }

    @Override
    public String toString() {
      return "ProofStatus{"
          + "description='" + description + '\''
          + '}';
    }
  }

  /**
   * Possible statuses of a full proof.
   */
  enum FullProofStatus implements ProofStatus {
    VALID("Proof has a valid structure"),
    INCORRECT_USER_KEY_PROOF("User collection proof is incorrect"),
    INCORRECT_STATE_HASH_PROOF("Aggregate state hash proof is incorect"),
    INCORRECT_BLOCK_PROOF("Block and precommit proof is incorrect");

    private final String description;

    FullProofStatus(String description) {
      this.description = description;
    }

    @Override
    public String getDescription() {
      return this.description;
    }

    @Override
    public String toString() {
      return "ProofStatus{"
          + "description='" + description + '\''
          + '}';
    }
  }
}
