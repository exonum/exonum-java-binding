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

package com.exonum.binding.service;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.proxy.AbstractCloseableNativeProxy;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.transaction.RawTransaction;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An Exonum node context. Allows to add transactions to Exonum network
 * and get a snapshot of the database state.
 */
public final class NodeProxy extends AbstractCloseableNativeProxy implements Node {

  private static final Logger logger = LogManager.getLogger(NodeProxy.class);

  /**
   * Creates a proxy of a node. Native code owns the node,
   * and, therefore, shall destroy the object.
   *
   * @param nativeHandle an implementation-specific reference to a native node
   */
  public NodeProxy(long nativeHandle) {
    super(nativeHandle, false);
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException if the node proxy is closed
   */
  @Override
  public HashCode submitTransaction(RawTransaction rawTransaction) throws InternalServerError {
    byte[] payload = rawTransaction.getPayload();
    short serviceId = rawTransaction.getServiceId();
    short transactionId = rawTransaction.getTransactionId();
    byte[] txMessageHash = nativeSubmit(getNativeHandle(), payload, serviceId, transactionId);

    return HashCode.fromBytes(txMessageHash);
  }

  /**
   * Submits a transaction into the network.
   *
   * @param nodeHandle a native handle to the native node object
   * @param payload a serialized transaction payload
   * @param serviceId an identifier of the service
   * @param transactionId an identifier of the transaction
   */
  private static native byte[] nativeSubmit(long nodeHandle, byte[] payload, short serviceId,
      short transactionId) throws InternalServerError;

  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException if the node proxy is closed
   */
  @Override
  public <ResultT> ResultT withSnapshot(Function<Snapshot, ResultT> snapshotFunction) {
    try (Cleaner cleaner = new Cleaner("NodeProxy#withSnapshot")) {
      long nodeNativeHandle = getNativeHandle();
      long snapshotNativeHandle = nativeCreateSnapshot(nodeNativeHandle);
      Snapshot snapshot = Snapshot.newInstance(snapshotNativeHandle, cleaner);
      return snapshotFunction.apply(snapshot);
    } catch (CloseFailuresException e) {
      logger.error(e);
      throw new RuntimeException(e);
    }
  }

  private native long nativeCreateSnapshot(long nativeHandle);

  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException if the node proxy is closed
   */
  @Override
  public PublicKey getPublicKey() {
    byte[] publicKey = nativeGetPublicKey(getNativeHandle());
    return PublicKey.fromBytes(publicKey);
  }

  private native byte[] nativeGetPublicKey(long nativeHandle);

  @Override
  protected void disposeInternal() {
    nativeFree(getNativeHandle());
  }

  private static native void nativeFree(long nodeNativeHandle);
}
