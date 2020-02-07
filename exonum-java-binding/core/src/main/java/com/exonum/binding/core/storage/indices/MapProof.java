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

import com.exonum.messages.proof.MapProofOuterClass;
import com.google.auto.value.AutoValue;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * A view of a {@link ProofMapIndexProxy}, i.e., a subset of its entries coupled
 * with a <em>proof</em>, which jointly allow restoring the
 * {@linkplain ProofMapIndexProxy#getIndexHash() index hash} of the map.
 * Apart from proving the existing entries in the map, MapProof can assert absence of certain keys
 * in the underlying index.
 * <!--
 * TODO: Improve docs: When verification arrives, explain how it is done.
 * -->
 *
 * @see ProofMapIndexProxy#getProof(Object, Object[])
 * @see <a href="../../blockchain/Blockchain.html#service-data-proof">Service Data Proofs</a>
 */
@AutoValue
public abstract class MapProof {

  /**
   * Returns the proof as a protobuf message.
   */
  public abstract MapProofOuterClass.MapProof getAsMessage();

  /**
   * Creates a new MapProof given the serialized map proof message.
   * @throws InvalidProtocolBufferException if the message is not
   *     {@link com.exonum.messages.proof.MapProofOuterClass.MapProof}
   */
  public static MapProof parseFrom(byte[] mapProofMessage) throws InvalidProtocolBufferException {
    return newInstance(MapProofOuterClass.MapProof.parseFrom(mapProofMessage));
  }

  /**
   * Creates a new MapProof given the map proof message.
   */
  public static MapProof newInstance(MapProofOuterClass.MapProof mapProofMessage) {
    return new AutoValue_MapProof(mapProofMessage);
  }
}
