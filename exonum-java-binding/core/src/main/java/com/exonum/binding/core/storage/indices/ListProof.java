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

import com.exonum.core.messages.ListProofOuterClass;
import com.google.auto.value.AutoValue;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * A list proof. It proves that certain elements are present in a proof list
 * of a certain size.
 * <!--
 * TODO: Improve docs when the whole proof support is ready: explain their place in the
 *   full proof creation process.
 * -->
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
  // todo: Consider renaming both to `parseFrom`?
  public static ListProof newInstance(byte[] proofMessage) throws InvalidProtocolBufferException {
    return newInstance(ListProofOuterClass.ListProof.parseFrom(proofMessage));
  }

  /**
   * Creates a new ListProof given the list proof message.
   */
  private static ListProof newInstance(ListProofOuterClass.ListProof proofMessage) {
    return new AutoValue_ListProof(proofMessage);
  }
}
