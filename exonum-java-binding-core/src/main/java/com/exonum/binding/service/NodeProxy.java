package com.exonum.binding.service;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.InternalServerError;
import com.exonum.binding.messages.InvalidTransactionException;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.proxy.AbstractNativeProxy;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.Snapshot;
import java.nio.ByteBuffer;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An Exonum node context. Allows to add transactions to Exonum network
 * and get a snapshot of the database state.
 */
public final class NodeProxy extends AbstractNativeProxy implements Node {

  private static final Logger logger = LogManager.getLogger(NodeProxy.class);

  /**
   * Creates a proxy of a node. Native code owns the node,
   * and, therefore, shall destroy the object.
   *
   * @param nativeHandle an implementation-specific reference to a native node
   */
  public NodeProxy(long nativeHandle) {
    // fixme: remove this comment when https://jira.bf.local/browse/ECR-251 is resolved
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
  public byte[] getPublicKey() {
    return nativeGetPublicKey(getNativeHandle());
  }

  private native byte[] nativeGetPublicKey(long nativeHandle);

  @Override
  protected void disposeInternal() {
    // no-op: this class is not responsible to destroy the corresponding native object
  }
}
