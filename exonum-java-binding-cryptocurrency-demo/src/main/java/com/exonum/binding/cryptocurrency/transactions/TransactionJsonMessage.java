/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.cryptocurrency.transactions;

/**
 * Data of a transaction message in JSON. Matches the
 * <a href="https://exonum.com/doc/architecture/serialization/#message-serialization">Exonum serialization format</a>
 * for messages.
 *
 * @param <BodyT> a type of object that is transferred as the message body
 */
@SuppressWarnings("unused")  // Fields are set through reflection by GSON.
final class TransactionJsonMessage<BodyT> {
  private byte protocol_version;
  private short service_id;
  private short message_id;
  private BodyT body;
  private String signature;

  byte getProtocolVersion() {
    return protocol_version;
  }

  short getServiceId() {
    return service_id;
  }

  short getMessageId() {
    return message_id;
  }

  BodyT getBody() {
    return body;
  }

  /**
   * Returns the signature in hex.
   */
  String getSignature() {
    return signature;
  }
}
