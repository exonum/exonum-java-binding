package com.exonum.binding.storage.indices;

import static com.exonum.binding.proxy.ProxyPreconditions.checkValid;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkElementIndex;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkStorageValue;

import com.exonum.binding.storage.database.View;
import java.util.NoSuchElementException;

/**
 * An abstract class for list indices implementing {@link ListIndex} interface.
 *
 * <p>Implements all methods from ListIndex.
 */
abstract class AbstractListIndexProxy extends AbstractIndexProxy implements ListIndex {

  AbstractListIndexProxy(long nativeHandle, View view) {
    super(nativeHandle, view);
  }

  @Override
  public final void add(byte[] e) {
    notifyModified();
    nativeAdd(getNativeHandle(), checkStorageValue(e));
  }

  @Override
  public final void set(long index, byte[] e) {
    checkElementIndex(index, size());
    notifyModified();
    nativeSet(getNativeHandle(), index, checkStorageValue(e));
  }

  @Override
  public final byte[] get(long index) {
    return nativeGet(getNativeHandle(), checkElementIndex(index, size()));
  }

  @Override
  public final byte[] getLast() {
    byte[] e = nativeGetLast(getNativeHandle());
    // This method does not check if the list is empty first to use only a single native call.
    if (e == null) {
      throw new NoSuchElementException("List is empty");
    }
    return e;
  }

  @Override
  public final void clear() {
    notifyModified();
    nativeClear(getNativeHandle());
  }

  @Override
  public final boolean isEmpty() {
    return nativeIsEmpty(getNativeHandle());
  }

  @Override
  public final long size() {
    return nativeSize(getNativeHandle());
  }

  @Override
  public final StorageIterator<byte[]> iterator() {
    return StorageIterators.createIterator(
        nativeCreateIter(getNativeHandle()),
        this::nativeIterNext,
        this::nativeIterFree,
        dbView,
        modCounter);
  }

  @Override
  protected final void disposeInternal() {
    checkValid(dbView);
    nativeFree(getNativeHandle());
  }

  abstract void nativeFree(long nativeHandle);

  abstract void nativeAdd(long nativeHandle, byte[] e);

  abstract void nativeSet(long nativeHandle, long index, byte[] e);

  abstract byte[] nativeGet(long nativeHandle, long index);

  abstract byte[] nativeGetLast(long nativeHandle);

  abstract void nativeClear(long nativeHandle);

  abstract boolean nativeIsEmpty(long nativeHandle);

  abstract long nativeSize(long nativeHandle);

  abstract long nativeCreateIter(long nativeHandle);

  abstract byte[] nativeIterNext(long iterNativeHandle);

  abstract void nativeIterFree(long iterNativeHandle);
}
