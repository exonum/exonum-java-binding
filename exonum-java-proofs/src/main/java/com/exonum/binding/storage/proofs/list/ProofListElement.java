/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.storage.proofs.list;

import com.exonum.binding.hash.Funnel;
import com.exonum.binding.hash.PrimitiveSink;

/**
 * Represents an element of a proof list: a leaf node in a list proof tree.
 */
public final class ProofListElement implements ListProof {

  private final byte[] element;

  /**
   * Creates a new ProofListElement.
   *
   * @param element an element of the list
   * @throws NullPointerException if the element is null
   */
  public ProofListElement(byte[] element) {
    this.element = element.clone();
  }

  @Override
  public void accept(ListProofVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * Returns the value of the element.
   */
  public byte[] getElement() {
    return element.clone();
  }

  public static Funnel<ProofListElement> funnel() {
    return ElementFunnel.INSTANCE;
  }

  enum ElementFunnel implements Funnel<ProofListElement> {
    INSTANCE {
      @Override
      public void funnel(ProofListElement from, PrimitiveSink into) {
        into.putBytes(from.element);
      }
    }
  }
}
