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
 *
 */

package com.exonum.binding.common.message;

/**
 * Message types used in Exonum framework.
 */
public enum MessageType {
  TRANSACTION(0x00, 0x00),
  STATUS(0x00, 0x01),
  CONNECT(0x00, 0x02),

  PRECOMMIT(0x01, 0x00),
  PROPOSE(0x01, 0x01),
  PREVOTE(0x01, 0x02),

  TRANSACTIONS_BATCH(0x02, 0x00),
  BLOCK_RESPONSE(0x02, 0x01),

  TRANSACTIONS_REQUEST(0x03, 0x00),
  PREVOTES_REQUEST(0x03, 0x01),
  PEERS_REQUEST(0x03, 0x02),
  BLOCK_REQUEST(0x03, 0x03);

  private final byte cls;
  private final byte tag;

  MessageType(int cls, int tag) {
    this.cls = (byte) cls;
    this.tag = (byte) tag;
  }

  /**
   * Represents message class byte value.
   */
  public byte cls() {
    return cls;
  }

  /**
   * Represents message tag byte value.
   */
  public byte tag() {
    return tag;
  }

  /**
   * Represents message class and tag bytes value.
   */
  public byte[] bytes() {
    return new byte[]{cls, tag};
  }

}
