package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.StoragePreconditions.checkElementIndex;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkIndexName;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkPositionIndex;

import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.proofs.list.ListProof;

/**
 * A proof list index proxy is a contiguous list of elements, capable of providing
 * cryptographic proofs that it contains a certain element at a particular position.
 * Non-null elements may be added to the end of the list only.
 *
 * <p>The proof list is implemented as a hash tree (Merkle tree).
 *
 * <p>The "destructive" methods of the list, i.e., those that change its contents,
 * are specified to throw {@link UnsupportedOperationException} if
 * this list has been created with a read-only database view.
 *
 * <p>All method arguments are non-null by default.
 *
 * <p>This class is not thread-safe and and its instances shall not be shared between threads.
 *
 * <p>As any native proxy, this list <em>must be closed</em> when no longer needed.
 * Subsequent use of the closed list is prohibited and will result in {@link IllegalStateException}.
 *
 * @see View
 */
public class ProofListIndexProxy extends AbstractListIndexProxy implements ListIndex {

  /**
   * Creates a new ProofListIndexProxy.
   *
   * @param name a unique alphanumeric non-empty identifier of this list in the underlying storage:
   *             [a-zA-Z0-9_]
   * @param view a database view. Must be valid.
   *             If a view is read-only, "destructive" operations are not permitted.
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the name is empty
   * @throws NullPointerException if any argument is null
   */
  public ProofListIndexProxy(String name, View view) {
    super(nativeCreate(checkIndexName(name), view.getViewNativeHandle()), view);
  }

  private static native long nativeCreate(String listName, long viewNativeHandle);

  /**
   * Returns a proof that an element exists at the specified index in this list.
   *
   * @param index the element index
   * @throws IndexOutOfBoundsException if the index is invalid
   * @throws IllegalStateException if this list is not valid
   */
  public ListProof getProof(long index) {
    checkElementIndex(index, size());
    return nativeGetProof(getNativeHandle(), index);
  }

  private native ListProof nativeGetProof(long nativeHandle, long index);

  /**
   * Returns a proof that some elements exist in the specified range in this list.
   *
   * @param from the index of the first element
   * @param to the index after the last element
   * @throws IndexOutOfBoundsException if the range is not valid
   * @throws IllegalStateException if this list is not valid
   */
  public ListProof getRangeProof(long from, long to) {
    long size = size();
    return nativeGetRangeProof(getNativeHandle(), checkElementIndex(from, size),
        checkPositionIndex(to, size));
  }

  private native ListProof nativeGetRangeProof(long nativeHandle, long from, long to);

  /**
   * Returns the root hash of the proof list.
   *
   * @throws IllegalStateException if this list is not valid
   */
  public byte[] getRootHash() {
    return nativeGetRootHash(getNativeHandle());
  }

  private native byte[] nativeGetRootHash(long nativeHandle);

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
