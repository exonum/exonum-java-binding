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

package com.exonum.binding.testkit;

import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.service.Node;
import com.exonum.binding.transaction.RawTransaction;
import java.util.OptionalInt;

/**
 * Context of the TestKit emulated node.
 */
public class EmulatedNode {

  private final OptionalInt validatorId;
  private final KeyPair serviceKeyPair;
  private final KeyPair consensusKeyPair;

  /**
   * Creates a context of an emulated node.
   *
   * @param validatorId validator id of the validator node, less or equal to 0 in case of an
   *     auditor node
   * @param serviceKeyPair service key pair of the node
   * @param consensusKeyPair consensus key pair of the node
   */
  public EmulatedNode(int validatorId, KeyPair serviceKeyPair, KeyPair consensusKeyPair) {
    this.validatorId = validatorId >= 0
        ? OptionalInt.of(validatorId)
        : OptionalInt.empty();
    this.serviceKeyPair = serviceKeyPair;
    this.consensusKeyPair = consensusKeyPair;
  }

  /**
   * Returns a node type - either {@link EmulatedNodeType.VALIDATOR} or
   * {@link EmulatedNodeType.AUDITOR}.
   */
  public EmulatedNodeType getNodeType() {
    return validatorId.isPresent() ? EmulatedNodeType.VALIDATOR : EmulatedNodeType.AUDITOR;
  }

  /**
   * Returns a validator id if this node is a validator or {@link OptionalInt.EMPTY} is this is an
   * auditor node.
   */
  public OptionalInt getValidatorId() {
    return validatorId;
  }

  /**
   * Returns a service key pair of this node. This key pair is used to sign transactions
   * {@linkplain Node#submitTransaction(RawTransaction)} produced} by the service itself.
   */
  public KeyPair getServiceKeyPair() {
    return serviceKeyPair;
  }

  /**
   * Returns a consensus key pair of this node. This key pair is used to sign consensus messages of
   * this node.
   */
  public KeyPair getConsensusKeyPair() {
    return consensusKeyPair;
  }
}
