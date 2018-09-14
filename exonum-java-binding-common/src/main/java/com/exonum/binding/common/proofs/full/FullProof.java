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

package com.exonum.binding.common.proofs.full;

import com.exonum.binding.common.proofs.full.unchecked.UncheckedCollectionProof;
import java.util.Map;

public class FullProof {
  private Map<Integer, UncheckedCollectionProof> userKeyProofs;
  private UncheckedCollectionProof stateHashProofs;
  private BlockProof blockProof;

  private FullProof(Map<Integer, UncheckedCollectionProof> userKeyProofs,
      UncheckedCollectionProof stateHashProofs, BlockProof blockProof) {
    this.userKeyProofs = userKeyProofs;
    this.stateHashProofs = stateHashProofs;
    this.blockProof = blockProof;
  }

  public Map<Integer, UncheckedCollectionProof> getUserKeyProofs() {
    return userKeyProofs;
  }

  public UncheckedCollectionProof getStateHashProofs() {
    return stateHashProofs;
  }

  public BlockProof getBlockProof() {
    return blockProof;
  }

}
