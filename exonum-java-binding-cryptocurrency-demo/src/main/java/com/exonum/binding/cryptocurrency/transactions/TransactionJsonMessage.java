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
final class TransactionJsonMessage<BodyT> {
  private byte protocolVersion;
  private short serviceId;
  private short messageId;
  private BodyT body;
  private String signature;

  byte getProtocolVersion() {
    return protocolVersion;
  }

  short getServiceId() {
    return serviceId;
  }

  short getMessageId() {
    return messageId;
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

  static <T> TransactionJsonMessageBuilder<T> builder() {
    return new TransactionJsonMessageBuilder<>();
  }

  static final class TransactionJsonMessageBuilder<BodyT> {
    private byte protocolVersion;
    private short serviceId;
    private short messageId;
    private BodyT body;
    private String signature;

    private TransactionJsonMessageBuilder() {
    }

    TransactionJsonMessageBuilder<BodyT> protocolVersion(byte protocolVersion) {
      this.protocolVersion = protocolVersion;
      return this;
    }

    TransactionJsonMessageBuilder<BodyT> serviceId(short serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    TransactionJsonMessageBuilder<BodyT> messageId(short messageId) {
      this.messageId = messageId;
      return this;
    }

    TransactionJsonMessageBuilder<BodyT> body(BodyT body) {
      this.body = body;
      return this;
    }

    TransactionJsonMessageBuilder<BodyT> signature(String signature) {
      this.signature = signature;
      return this;
    }

    TransactionJsonMessage<BodyT> build() {
      TransactionJsonMessage<BodyT> message = new TransactionJsonMessage<>();
      message.body = this.body;
      message.protocolVersion = this.protocolVersion;
      message.serviceId = this.serviceId;
      message.signature = this.signature;
      message.messageId = this.messageId;
      return message;
    }
  }
}
