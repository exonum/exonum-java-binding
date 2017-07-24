package com.exonum.binding.proxy;

import static com.exonum.binding.proxy.StoragePreconditions.checkElementIndex;
import static com.exonum.binding.proxy.StoragePreconditions.checkIndexPrefix;
import static com.exonum.binding.proxy.StoragePreconditions.checkStorageValue;
import static com.exonum.binding.proxy.StoragePreconditions.checkValid;

import java.util.NoSuchElementException;

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
public class ProofListIndexProxy extends AbstractIndexProxy implements ListIndex {

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
  ProofListIndexProxy(byte[] prefix, View view) {
    super(nativeCreate(checkIndexPrefix(prefix), view.getNativeHandle()), view);
  }

  @Override
  public void add(byte[] e) {
    notifyModified();
    nativeAdd(getNativeHandle(), checkStorageValue(e));
  }

  @Override
  public void set(long index, byte[] e) {
    checkElementIndex(index, size());
    notifyModified();
    nativeSet(getNativeHandle(), index, checkStorageValue(e));

  }

  @Override
  public byte[] get(long index) {
    return nativeGet(getNativeHandle(), checkElementIndex(index, size()));
  }

  @Override
  public byte[] getLast() {
    byte[] e = nativeGetLast(getNativeHandle());
    // This method does not check if the list is empty first to use only a single native call
    // in an optimistic scenario (= non-empty list).
    if (e == null) {
      throw new NoSuchElementException("List is empty");
    }
    return e;
  }

  @Override
  public void clear() {
    notifyModified();
    nativeClear(getNativeHandle());
  }

  @Override
  public boolean isEmpty() {
    return nativeIsEmpty(getNativeHandle());
  }

  @Override
  public long size() {
    return nativeSize(getNativeHandle());
  }

  @Override
  public RustIter<byte[]> iterator() {
    return new ConfigurableRustIter<>(nativeCreateIter(getNativeHandle()),
        this::nativeIterNext,
        this::nativeIterFree,
        dbView,
        modCounter);
  }

  @Override
  void disposeInternal() {
    checkValid(dbView);
    nativeFree(getNativeHandle());
  }

  private static native long nativeCreate(byte[] listPrefix, long viewNativeHandle);

  private native void nativeFree(long nativeHandle);

  private native void nativeAdd(long nativeHandle, byte[] e);

  private native void nativeSet(long nativeHandle, long index, byte[] e);

  private native byte[] nativeGet(long nativeHandle, long index);

  private native byte[] nativeGetLast(long nativeHandle);

  private native void nativeClear(long nativeHandle);

  private native boolean nativeIsEmpty(long nativeHandle);

  private native long nativeSize(long nativeHandle);

  private native long nativeCreateIter(long nativeHandle);

  private native byte[] nativeIterNext(long iterNativeHandle);

  private native void nativeIterFree(long iterNativeHandle);
}
