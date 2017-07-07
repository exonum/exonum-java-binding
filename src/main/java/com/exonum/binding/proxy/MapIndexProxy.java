package com.exonum.binding.proxy;

import static com.exonum.binding.proxy.StoragePreconditions.checkIndexPrefix;
import static com.exonum.binding.proxy.StoragePreconditions.checkStorageKey;
import static com.exonum.binding.proxy.StoragePreconditions.checkStorageValue;
import static com.exonum.binding.proxy.StoragePreconditions.checkValid;

import com.exonum.binding.annotations.ImproveDocs;

@ImproveDocs(
    assignee = "dt",
    reason = "consider using exonum::storage docs + java.util.Map as a reference"
)
public class MapIndexProxy extends AbstractNativeProxy {
  // TODO: consider moving 'dbView' to a super class as 'parents'
  //       (= objects that must not be deleted before this)
  private final View dbView;

  @ImproveDocs(assignee = "dt")
  public MapIndexProxy(View view, byte[] prefix) {
    super(nativeCreate(view.getNativeHandle(), checkIndexPrefix(prefix)),
        true);
    this.dbView = view;
  }

  public void put(byte[] key, byte[] value) {
    nativePut(checkStorageKey(key), checkStorageValue(value), nativeHandle);
  }

  public byte[] get(byte[] key) {
    return nativeGet(checkStorageKey(key), nativeHandle);
  }

  public void remove(byte[] key) {
    nativeRemove(checkStorageKey(key), nativeHandle);
  }

  public void clear() {
    nativeClear(nativeHandle);
  }

  @Override
  void disposeInternal() {
    checkValid(dbView);
    nativeFree(nativeHandle);
  }

  private static native long nativeCreate(long viewNativeHandle, byte[] prefix);

  private native void nativePut(byte[] key, byte[] value, long nativeHandle);

  private native byte[] nativeGet(byte[] key, long nativeHandle);

  private native void nativeRemove(byte[] key, long nativeHandle);

  private native void nativeClear(long nativeHandle);

  private native void nativeFree(long nativeHandle);
}
