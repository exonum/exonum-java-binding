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

package com.exonum.binding.common.proofs.list.adapter;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.proofs.full.checked.CheckedListProof;
import com.exonum.binding.common.proofs.list.ListProofElement;
import com.exonum.binding.common.proofs.model.ProofStatus;
import java.util.NavigableMap;

public class CheckedListProofAdapter implements CheckedListProof {
  private final NavigableMap<Long, ListProofElement> elements;
  private final HashCode rootHash;

  public CheckedListProofAdapter(NavigableMap<Long, ListProofElement> elements, HashCode rootHash) {
    this.elements = elements;
    this.rootHash = rootHash;
  }

  @Override
  public NavigableMap<Long, ListProofElement> getElements() {
    return elements;
  }

  @Override
  public HashCode getRootHash() {
    return rootHash;
  }

  //TODO need to refactor status code in ListProofValidator getReason() to work with enums not
  // strings.
  @Override
  public ProofStatus getStatus() {
    return null;
  }
}
