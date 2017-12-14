package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.StoragePreconditions.checkElementIndex;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkIndexName;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkStorageValue;

import com.exonum.binding.storage.database.View;
import java.util.Optional;

public class SparseListIndexProxy extends AbstractIndexProxy {

  SparseListIndexProxy(String name, View view) {
    super(nativeCreateSparseList(checkIndexName(name), view.getViewNativeHandle()), view);
  }

  private static native long nativeCreateSparseList(String name, long viewNativeHandle);

  public void add(byte[] element) {
    nativeAdd(getNativeHandle(), checkStorageValue(element));
  }

  private native void nativeAdd(long nativeHandle, byte[] element);

  /**
   * Returns the number of set elements (excluding non-empty).
   */
  public long size() {
    return nativeSize(getNativeHandle());
  }

  private native long nativeSize(long nativeHandle);

  public void set(long index, byte[] element) {
    nativeSet(getNativeHandle(), index, checkStorageValue(element));
  }

  private native void nativeSet(long nativeHandle, long index, byte[] element);

  public Optional<byte[]> get(long index) {
    checkElementIndex(index, Long.MAX_VALUE);
    return Optional.ofNullable(nativeGet(getNativeHandle(), index));
  }

  // byte[] -- if it's set; null -- if no such element.
  private native byte[] nativeGet(long nativeHandle, long index);

  @Override
  protected void disposeInternal() {
    nativeFree(getNativeHandle());
  }

  private native void nativeFree(long nativeHandle);
}
