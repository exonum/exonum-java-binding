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

import static com.exonum.binding.common.proofs.model.ProofStatus.FullProofStatus.VALID;

import com.exonum.binding.common.proofs.full.checked.CheckedListProof;
import com.exonum.binding.common.proofs.full.checked.CheckedMapProof;
import com.exonum.binding.common.proofs.model.ProofStatus;
import com.exonum.binding.common.serialization.Serializer;
import java.util.List;

/**
 * A validator of full proofs.
 *
 */
public final class FullProofValidatorImpl implements FullProofValidator {
  private FullProof fullProof;
  private ProofStatus.FullProofStatus status;

  private FullProofValidatorImpl(ProofStatus.FullProofStatus status, FullProof fullProof) {
    this.status = status;
    this.fullProof = fullProof;
  }

  @Override
  public FullProofValidator check(FullProof fullProof, int collectionId,
      List<byte[]> expectedKeys) {
    ProofStatus.FullProofStatus proofStatus = someCheck(fullProof, collectionId, expectedKeys);

    return new FullProofValidatorImpl(proofStatus, fullProof);
  }

  @Override
  public CheckedListProof getCheckedListProof(int collectionId) {
    //TODO need's to be implemented
    return null;
  }

  @Override
  public CheckedMapProof getCheckedMapProof(int collectionId) {
    //TODO need's to be implemented
    return null;
  }

  @Override
  public CheckedListProof getSerializedCheckedListProof(int collectionId, Serializer serializer) {
    //TODO need's to be implemented
    return null;
  }

  @Override
  public CheckedMapProof getSerializedCheckedMapProof(int collectionId, Serializer keySerializer,
      Serializer valueSerializer) {
    //TODO need's to be implemented
    return null;
  }

  private static ProofStatus.FullProofStatus someCheck(FullProof fullProof, int collectionId,
      List<byte[]> expectedKeys) {
    /**
     * TODO
     * Here we need to perform all required checks on all proof collections
     *
     * - Check user cpllections and get rootHash from them
     * - Check stateHash
     * - Check each user collection for stateHash
     * - Check that block is correct, and contains computed stateHash
     * - Check precommit messages
     */

    return VALID;
  }

  public ProofStatus.FullProofStatus getStatus() {
    return status;
  }

  @Override
  public String toString() {
    return "FullProofValidatorImpl{"
        + "userKeyProofs =" + fullProof.getUserKeyProofs()
        + ", stateHashProofs=" + fullProof.getStateHashProofs()
        + ", blockProof=" + fullProof.getBlockProof()
        + '}';
  }

}
