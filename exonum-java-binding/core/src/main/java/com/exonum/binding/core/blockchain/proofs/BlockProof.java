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

import com.exonum.messages.core.Proofs;
import com.google.auto.value.AutoValue;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * A block with a proof. A proof contains signed precommit messages from the network validators
 * that agreed to commit this block.
 *
 * <p>A block proof can be used independently or as a part of {@linkplain IndexProof index proof};
 * or transaction proof.
 *
 * @see <a href="../Blockchain.html#block-proof">Block Proof Creation</a>
 * @see com.exonum.binding.core.blockchain.Block
 */
@AutoValue
public abstract class BlockProof {

  /**
   * Returns the proof as a protobuf message.
   */
  public abstract Proofs.BlockProof getAsMessage();

  /**
   * Parses a serialized block proof message.
   * @throws InvalidProtocolBufferException if the message is not
   *     {@link com.exonum.messages.core.Proofs.BlockProof}
   */
  public static BlockProof parseFrom(byte[] blockProof)
      throws InvalidProtocolBufferException {
    Proofs.BlockProof parsed = Proofs.BlockProof.parseFrom(blockProof);
    return newInstance(parsed);
  }

  /**
   * Creates a new BlockProof given the block proof message.
   */
  public static BlockProof newInstance(Proofs.BlockProof proofMessage) {
    return new AutoValue_BlockProof(proofMessage);
  }
}
