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

import com.exonum.core.messages.MapProofOuterClass;
import com.google.auto.value.AutoValue;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * A proof that there are some values mapped to the keys or that there are no such
 * mappings.
 * <!--
 * TODO: Improve docs when the whole proof support is ready: explain their place in the
 *   full proof creation process.
 * -->
 *
 * @see ProofMapIndexProxy#getProof(Object, Object[])
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
   *     {@link com.exonum.core.messages.MapProofOuterClass.MapProof}
   */
  public static MapProof newInstance(byte[] mapProofMessage) throws InvalidProtocolBufferException {
    return newInstance(MapProofOuterClass.MapProof.parseFrom(mapProofMessage));
  }

  /**
   * Creates a new MapProof given the map proof message.
   */
  public static MapProof newInstance(MapProofOuterClass.MapProof mapProofMessage) {
    return new AutoValue_MapProof(mapProofMessage);
  }
}
