/*
 * Copyright 2020 The Exonum Team
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

package com.exonum.binding.core.runtime;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.transaction.RawTransaction;
import java.util.function.Function;

/**
 * NodeProxy adapter for a particular service.
 */
class ServiceNodeProxy implements Node {

  private final NodeProxy node;
  private final BlockchainDataFactory blockchainDataFactory;
  private final String instanceName;
  private boolean closed;

  ServiceNodeProxy(NodeProxy node, BlockchainDataFactory blockchainDataFactory,
      String instanceName) {
    this.node = checkNotNull(node);
    this.blockchainDataFactory = blockchainDataFactory;
    this.instanceName = instanceName;
    this.closed = false;
  }

  @Override
  public HashCode submitTransaction(RawTransaction rawTransaction) {
    return node().submitTransaction(rawTransaction);
  }

  @Override
  public <ResultT> ResultT withBlockchainData(Function<BlockchainData, ResultT> snapshotFunction) {
    return node().withSnapshot(snapshotFunction
        .compose(snapshot -> blockchainDataFactory.fromRawAccess(snapshot, instanceName)));
  }

  @Override
  public PublicKey getPublicKey() {
    return node().getPublicKey();
  }

  /**
   * Closes an access to the node. After calling this method subsequent calling
   * {@link #submitTransaction(RawTransaction)} or {@link #withBlockchainData(Function)} methods
   * will cause {@link IllegalStateException}.
   */
  @Override
  public void close() {
    this.closed = true;
  }

  private NodeProxy node() {
    checkState(!closed, "Node access is closed");
    return node;
  }
}
