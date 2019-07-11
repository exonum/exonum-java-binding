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

package com.exonum.client;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.client.response.Block;
import java.time.ZonedDateTime;

public final class Blocks {
  static final String BLOCK_1_JSON = "{\n"
      + "        'proposer_id': 1,\n"
      + "        'height': 10,\n"
      + "        'tx_count': 1,\n"
      + "        'prev_hash': 'abc8',\n"
      + "        'tx_hash': 'cd5a',\n"
      + "        'state_hash': 'efa2'\n"
      + "    }";
  static final String BLOCK_1_TIME = "2019-02-10T10:00:00.037255Z";
  static final Block BLOCK_1 = Block.builder()
      .proposerId(1)
      .height(10)
      .numTransactions(1)
      .previousBlockHash(HashCode.fromString("abc8"))
      .txRootHash(HashCode.fromString("cd5a"))
      .stateHash(HashCode.fromString("efa2"))
      .commitTime(ZonedDateTime.parse(BLOCK_1_TIME))
      .build();
  static final Block BLOCK_1_WITHOUT_TIME = Block.builder()
      .proposerId(1)
      .height(10)
      .numTransactions(1)
      .previousBlockHash(HashCode.fromString("abc8"))
      .txRootHash(HashCode.fromString("cd5a"))
      .stateHash(HashCode.fromString("efa2"))
      .build();

  static final String BLOCK_2_JSON = "{\n"
      + "        'proposer_id': 2,\n"
      + "        'height': 25,\n"
      + "        'tx_count': 1,\n"
      + "        'prev_hash': 'aa4e',\n"
      + "        'tx_hash': 'dcb0',\n"
      + "        'state_hash': 'e4ea'\n"
      + "    }";
  static final String BLOCK_2_TIME = "2019-02-10T11:00:00.321051Z";
  static final Block BLOCK_2 = Block.builder()
      .proposerId(2)
      .height(25)
      .numTransactions(1)
      .previousBlockHash(HashCode.fromString("aa4e"))
      .txRootHash(HashCode.fromString("dcb0"))
      .stateHash(HashCode.fromString("e4ea"))
      .commitTime(ZonedDateTime.parse(BLOCK_2_TIME))
      .build();
  static final Block BLOCK_2_WITHOUT_TIME = Block.builder()
      .proposerId(2)
      .height(25)
      .numTransactions(1)
      .previousBlockHash(HashCode.fromString("aa4e"))
      .txRootHash(HashCode.fromString("dcb0"))
      .stateHash(HashCode.fromString("e4ea"))
      .build();

  static final String BLOCK_3_JSON = "{\n"
      + "        'proposer_id': 3,\n"
      + "        'height': 78,\n"
      + "        'tx_count': 1,\n"
      + "        'prev_hash': '7183',\n"
      + "        'tx_hash': '362b',\n"
      + "        'state_hash': '00cc'\n"
      + "    }";
  static final String BLOCK_3_TIME = "2019-02-10T12:00:00.321051Z";
  static final Block BLOCK_3 = Block.builder()
      .proposerId(3)
      .height(78)
      .numTransactions(1)
      .previousBlockHash(HashCode.fromString("7183"))
      .txRootHash(HashCode.fromString("362b"))
      .stateHash(HashCode.fromString("00cc"))
      .commitTime(ZonedDateTime.parse(BLOCK_3_TIME))
      .build();
  static final Block BLOCK_3_WITHOUT_TIME = Block.builder()
      .proposerId(3)
      .height(78)
      .numTransactions(1)
      .previousBlockHash(HashCode.fromString("7183"))
      .txRootHash(HashCode.fromString("362b"))
      .stateHash(HashCode.fromString("00cc"))
      .build();

  /** Creates a block builder with some pre-configured values and empty commit time. */
  public static Block.BlockBuilder aBlock() {
    return Block.builder()
        .proposerId(1)
        .height(1)
        .numTransactions(0)
        .previousBlockHash(HashCode.fromLong(0x1234))
        .txRootHash(HashCode.fromLong(0x5678))
        .stateHash(HashCode.fromLong(0x9ABC));
  }

  private Blocks() {
  }
}
