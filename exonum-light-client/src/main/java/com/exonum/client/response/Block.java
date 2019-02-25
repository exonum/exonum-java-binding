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

package com.exonum.client.response;

import com.exonum.binding.common.hash.HashCode;
import com.google.gson.annotations.SerializedName;
import lombok.Value;

@Value
public class Block {

  /**
   * Identifier of the leader node which has proposed the block.
   */
  @SerializedName("proposer_id")
  int proposerId;

  /**
   * Returns the height of this block which is a distance between the last block and the "genesis"
   * block. Genesis block has 0 height. Therefore, the blockchain height is equal to
   * the number of blocks plus one.
   *
   * <p>The height also identifies each block in the blockchain.
   *
   */
  long height;

  /**
   * Number of transactions in this block.
   */
  @SerializedName("tx_count")
  int numTransactions;

  /**
   * Hash link to the previous block in the blockchain.
   */
  @SerializedName("prev_hash")
  HashCode previousBlockHash;

  /**
   * Root hash of the Merkle tree of transactions in this block.
   */
  @SerializedName("tx_hash")
  HashCode txRootHash;

  /**
   * Hash of the blockchain state after applying transactions in the block.
   */
  @SerializedName("state_hash")
  HashCode stateHash;

}
