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

import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.transaction.RawTransaction;
import java.util.function.Function;

/**
 * Node decorator which can restrict access to the node by calling {@link #restrictAccess()} method.
 */
class RestrictingNodeDecorator implements Node {

  private final Node node;
  private boolean accessAllowed;

  RestrictingNodeDecorator(Node node) {
    this.node = node;
    this.accessAllowed = true;
  }

  @Override
  public HashCode submitTransaction(RawTransaction rawTransaction) {
    checkAccess();
    return node.submitTransaction(rawTransaction);
  }

  @Override
  public <ResultT> ResultT withSnapshot(Function<Snapshot, ResultT> snapshotFunction) {
    checkAccess();
    return node.withSnapshot(snapshotFunction);
  }

  @Override
  public PublicKey getPublicKey() {
    return node.getPublicKey();
  }

  /**
   * Restricts access to the node. After calling this method subsequent calling
   * {@link #submitTransaction(RawTransaction)} or {@link #withSnapshot(Function)} methods
   * will cause {@link IllegalStateException}.
   */
  void restrictAccess() {
    this.accessAllowed = false;
  }

  private void checkAccess() {
    checkState(accessAllowed, "Node access is not allowed");
  }

}
