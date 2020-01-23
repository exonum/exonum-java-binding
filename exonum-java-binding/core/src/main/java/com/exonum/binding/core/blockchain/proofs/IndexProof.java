/*
 * Copyright 2020 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.core.blockchain.proofs;

import com.exonum.binding.core.storage.indices.MapProof;
import com.exonum.core.messages.Proofs;
import com.google.auto.value.AutoValue;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Proof of authenticity for a single index in the database.
 *
 * <p>It is comprised of a {@link BlockProof} and a {@link MapProof} from the collection aggregating
 * the index hashes of proof indexes for an index with a certain full name.
 *
 * <p>If an index does not exist in the database, then the MapProof will prove its absence.
 *
 * @see <a href="../Blockchain.html#service-data-proof">Service Data Proofs</a>
 * @see com.exonum.binding.core.service.Schema
 */
@AutoValue
public abstract class IndexProof {

  /** Returns the proof as a protobuf message. */
  public abstract Proofs.IndexProof getAsMessage();

  /**
   * Parses a serialized index proof message.
   *
   * @throws InvalidProtocolBufferException if the message is not {@link
   *     com.exonum.core.messages.Proofs.IndexProof}
   */
  public static IndexProof parseFrom(byte[] indexProof) throws InvalidProtocolBufferException {
    Proofs.IndexProof parsed = Proofs.IndexProof.parseFrom(indexProof);
    return newInstance(parsed);
  }

  /** Creates a new IndexProof given the index proof message. */
  public static IndexProof newInstance(Proofs.IndexProof proofMessage) {
    return new AutoValue_IndexProof(proofMessage);
  }
}
