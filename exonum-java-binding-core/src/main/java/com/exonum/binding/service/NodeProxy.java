package com.exonum.binding.service;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.InternalServerError;
import com.exonum.binding.messages.InvalidTransactionException;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.proxy.AbstractNativeProxy;
import com.exonum.binding.storage.database.Snapshot;
import java.nio.ByteBuffer;

/**
 * An Exonum node context. Allows to add transactions to Exonum network
 * and get a snapshot of the database state.
 */
public class NodeProxy extends AbstractNativeProxy implements Node {

  /**
   * Creates a proxy of a node. Native code owns the node,
   * and, therefore, shall destroy the object.
   *
   * @param nativeHandle an implementation-specific reference to a native node
   */
  protected NodeProxy(long nativeHandle) {
    // fixme: remove this comment when https://jira.bf.local/browse/EEN-27 is resolved
    super(nativeHandle, false);
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException if the node proxy is closed
   */
  @Override
  public void submitTransaction(Transaction transaction)
      throws InvalidTransactionException, InternalServerError {
    BinaryMessage message = transaction.getMessage();
    ByteBuffer messageBuffer = message.getMessage();

    // Currently this method and the native code support only array-backed ByteBuffers.
    checkArgument(messageBuffer.hasArray(),
        "The byte buffer does not provide an array (it is either a direct or read-only). "
            + "direct=%s, ro=%s", messageBuffer.isDirect(), messageBuffer.isReadOnly());

    byte[] data = messageBuffer.array();
    int offset = messageBuffer.arrayOffset();
    int size = messageBuffer.remaining();
    nativeSubmit(getNativeHandle(), transaction, data, offset, size);
  }

  /**
   * Submits a transaction into the network.
   *
   * @param nodeHandle a native handle to the native node object
   * @param transaction a transaction to submit
   * @param message an array containing the transaction message
   * @param offset an offset from which the message starts
   * @param size a size of the message in bytes
   */
  private static native void nativeSubmit(long nodeHandle, Transaction transaction,
                                          byte[] message, int offset, int size)
      throws InvalidTransactionException, InternalServerError;

  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException if the node proxy is closed
   */
  @Override
  public Snapshot createSnapshot() {
    return new Snapshot(nativeCreateSnapshot(getNativeHandle()));
  }

  private native long nativeCreateSnapshot(long nativeHandle);

  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException if the node proxy is closed
   */
  @Override
  public byte[] getPublicKey() {
    return nativeGetPublicKey(getNativeHandle());
  }

  private native byte[] nativeGetPublicKey(long nativeHandle);

  @Override
  protected void disposeInternal() {
    // no-op: this class is not responsible to destroy the corresponding native object
  }
}
