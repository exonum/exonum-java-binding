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

package com.exonum.binding.qaservice;

import com.exonum.binding.common.blockchain.Block;
import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.configuration.StoredConfiguration;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.service.Service;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * A simple service for QA purposes.
 */
public interface QaService extends Service {

  short ID = 127;
  String NAME = "ejb-qa-service";

  HashCode submitCreateCounter(String counterName);

  HashCode submitIncrementCounter(long requestSeed, HashCode counterId);

  HashCode submitInvalidTx();

  HashCode submitInvalidThrowingTx();

  HashCode submitValidThrowingTx(long requestSeed);

  HashCode submitValidErrorTx(long requestSeed, byte errorCode, @Nullable String description);

  HashCode submitUnknownTx();

  Optional<Counter> getValue(HashCode counterId);

  Height getHeight();

  List<HashCode> getAllBlockHashes();

  List<HashCode> getBlockTransactions(long height);

  List<HashCode> getBlockTransactions(HashCode blockId);

  Map<HashCode, TransactionMessage> getTxMessages();

  Map<HashCode, TransactionResult> getTxResults();

  TransactionResult getTxResult(HashCode messageHash);

  Map<HashCode, TransactionLocation> getTxLocations();

  TransactionLocation getTxLocation(HashCode messageHash);

  Map<HashCode, Block> getBlocks();

  List<Block> getBlocksByHeight();

  Block getBlock(HashCode blockHash);

  Block getLastBlock();

  StoredConfiguration getActualConfiguration();
}
