/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.core.storage.indices;

import com.exonum.messages.proof.ListProofOuterClass;
import com.google.auto.value.AutoValue;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * A view of a {@link ProofListIndexProxy}, i.e., a subset of its elements coupled
 * with a <em>proof</em>, which jointly allow restoring the
 * {@linkplain ProofListIndexProxy#getIndexHash() index hash} of the list. Apart from proving
 * elements in the list, ListProof can assert that the list is shorter than the requested
 * range of indexes.
 * <!--
 * TODO: Improve docs when verification arrives: explain how it is done.
 * -->
 *
 * @see ProofListIndexProxy#getProof(long)
 * @see ProofListIndexProxy#getRangeProof(long, long)
 * @see <a href="../../blockchain/Blockchain.html#service-data-proof">Service Data Proofs</a>
 */
@AutoValue
public abstract class ListProof {

  /**
   * Returns the proof as a protobuf message.
   */
  public abstract ListProofOuterClass.ListProof getAsMessage();

  /**
   * Creates a new ListProof given the serialized map proof message.
   * @throws InvalidProtocolBufferException if the message is not
   *     {@link com.exonum.core.messages.MapProofOuterClass.MapProof}
   */
  public static ListProof parseFrom(byte[] proofMessage) throws InvalidProtocolBufferException {
    return newInstance(ListProofOuterClass.ListProof.parseFrom(proofMessage));
  }

  /**
   * Creates a new ListProof given the list proof message.
   */
  private static ListProof newInstance(ListProofOuterClass.ListProof proofMessage) {
    return new AutoValue_ListProof(proofMessage);
  }
}
