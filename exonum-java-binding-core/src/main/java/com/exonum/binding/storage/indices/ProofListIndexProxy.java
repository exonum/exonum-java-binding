package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.StoragePreconditions.checkIndexPrefix;

import com.exonum.binding.storage.database.View;

/**
 * A proof list index proxy is a contiguous list of elements, capable of providing
 * cryptographic proofs that it contains a certain element at a particular position.
 * Elements may be added to the end of the list only.
 *
 * <p>The proof list is implemented as a hash tree (Merkle tree).
 *
 * <p>The "destructive" methods of the list, i.e., those that change its contents,
 * are specified to throw {@link UnsupportedOperationException} if
 * this list has been created with a read-only database view.
 *
 * <p>This list implementation does not permit null elements.
 *
 * <p>As any native proxy, this list <em>must be closed</em> when no longer needed.
 * Subsequent use of the closed list is prohibited and will result in {@link IllegalStateException}.
 */
public class ProofListIndexProxy extends AbstractListIndexProxy implements ListIndex {

  /**
   * Creates a new ProofListIndexProxy.
   *
   * @param prefix a unique identifier of this list in the underlying storage
   * @param view a database view. Must be valid.
   *             If a view is read-only, "destructive" operations are not permitted.
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the prefix has zero size
   * @throws NullPointerException if any argument is null
   */
  public ProofListIndexProxy(byte[] prefix, View view) {
    super(nativeCreate(checkIndexPrefix(prefix), view.getViewNativeHandle()), view);
  }

  private static native long nativeCreate(byte[] listPrefix, long viewNativeHandle);

  @Override
  native void nativeFree(long nativeHandle);

  @Override
  native void nativeAdd(long nativeHandle, byte[] e);

  @Override
  native void nativeSet(long nativeHandle, long index, byte[] e);

  @Override
  native byte[] nativeGet(long nativeHandle, long index);

  @Override
  native byte[] nativeGetLast(long nativeHandle);

  @Override
  native void nativeClear(long nativeHandle);

  @Override
  native boolean nativeIsEmpty(long nativeHandle);

  @Override
  native long nativeSize(long nativeHandle);

  @Override
  native long nativeCreateIter(long nativeHandle);

  @Override
  native byte[] nativeIterNext(long iterNativeHandle);

  @Override
  native void nativeIterFree(long iterNativeHandle);
}
