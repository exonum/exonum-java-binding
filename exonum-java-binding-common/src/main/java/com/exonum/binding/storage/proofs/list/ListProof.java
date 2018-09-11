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

/**
 * Represents a proof that some elements exist in a ProofList at certain positions.
 */
public interface ListProof {

  /**
   * Applies the visitor to this proof node.
   *
   * <p>Most implementations simply call {@code visitor.visit(this);}
   *
   * @param visitor a visitor to apply to this node
   */
  void accept(ListProofVisitor visitor);
}
