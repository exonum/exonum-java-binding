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

/**
 * Possible statuses of a checked map proof.
 */
public enum ProofStatus {
  CORRECT("Proof has a valid structure"),
  NON_TERMINAL_NODE("Proof entry in a singleton proof is of branch type (must be a leaf)"),
  INVALID_ORDER("Proof entries are placed in the wrong order"),
  DUPLICATE_PATH("There are entries with duplicate keys"),
  EMBEDDED_PATH("One key in the proof is a prefix of another key");

  final String description;

  ProofStatus(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return "ProofStatus{"
        + "description='" + description + '\''
        + '}';
  }
}
