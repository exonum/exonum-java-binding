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

package com.exonum.binding.common.serialization.blockchain;

import com.exonum.binding.common.blockchain.Block;
import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.serialization.Serializer;

/**
 * A collection of pre-defined serializers related to the blockchain entities.
 */
public final class BlockchainSerializers {

  /**
   * Returns a serializer of blocks.
   */
  public static Serializer<Block> block() {
    return BlockSerializer.INSTANCE;
  }

  /**
   * Returns a serializer of transaction locations.
   */
  public static Serializer<TransactionLocation> transactionLocation() {
    return TransactionLocationSerializer.INSTANCE;
  }

  /**
   * Returns a serializer of transaction execution results.
   */
  public static Serializer<TransactionResult> transactionResult() {
    return TransactionResultSerializer.INSTANCE;
  }

}
