package com.exonum.binding;

import com.exonum.binding.proxy.AbstractNativeProxy;
import com.exonum.binding.storage.database.Snapshot;

/**
 * An Exonum blockchain. It provides methods to access its current state.
 */
public final class Blockchain extends AbstractNativeProxy {

  /**
   * Creates a native proxy of a blockchain.
   *
   * @param nativeHandle an implementation-specific reference to a native object
   * @param owningHandle true if this proxy is responsible to release any native resources
   */
  protected Blockchain(
      long nativeHandle,
      boolean owningHandle // fixme: remove the argument when https://jira.bf.local/browse/EEN-27 is resolved
  ) {
    super(nativeHandle, owningHandle);
  }

  /**
   * Creates a new snapshot of the blockchain state.
   *
   * <p>The caller is responsible to <strong>close</strong> the snapshot
   * to destroy the corresponding native objects.
   *
   * @return a new snapshot
   * @see Snapshot
   */
  public Snapshot createSnapshot() {
    long nativeHandle = nativeCreateSnapshot(getNativeHandle());
    return new Snapshot(nativeHandle);
  }

  private native long nativeCreateSnapshot(long nativeHandle);

  /**
   * todo: see https://jira.bf.local/browse/EEN-27 when implementing nativeFree.
   */
  @Override
  protected void disposeInternal() {
    nativeFree(getNativeHandle());
  }

  private native void nativeFree(long nativeHandle);
}
