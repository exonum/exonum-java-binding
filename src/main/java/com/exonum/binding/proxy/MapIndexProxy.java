package com.exonum.binding.proxy;

import static com.exonum.binding.proxy.StoragePreconditions.checkCanModify;
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

  private final ViewModificationCounter modCounter;

  @ImproveDocs(assignee = "dt")
  public MapIndexProxy(View view, byte[] prefix) {
    super(nativeCreate(view.getNativeHandle(), checkIndexPrefix(prefix)),
        true);
    this.dbView = view;
    modCounter = ViewModificationCounter.getInstance();
  }

  public void put(byte[] key, byte[] value) {
    notifyModified();
    nativePut(checkStorageKey(key), checkStorageValue(value), getNativeHandle());
  }

  public byte[] get(byte[] key) {
    return nativeGet(checkStorageKey(key), getNativeHandle());
  }

  public void remove(byte[] key) {
    notifyModified();
    nativeRemove(checkStorageKey(key), getNativeHandle());
  }

  /**
   * Returns an iterator over keys. Must be closed.
   */
  // TODO(dt): consider creating a subclass (RustByteIter) so that you don't have to put a
  // type parameter?
  public RustIter<byte[]> keys() {
    return new ConfigurableRustIter<>(nativeCreateKeysIter(getNativeHandle()),
        this::nativeKeysIterNext,
        this::nativeKeysIterFree,
        dbView,
        modCounter);
  }

  /**
   * Returns an iterator over values. Must be closed.
   */
  public RustIter<byte[]> values() {
    return new ConfigurableRustIter<>(nativeCreateValuesIter(getNativeHandle()),
          this::nativeValuesIterNext,
          this::nativeValuesIterFree,
          dbView,
          modCounter);
  }

  public void clear() {
    notifyModified();
    nativeClear(getNativeHandle());
  }

  private void notifyModified() {
    modCounter.notifyModified(checkCanModify(dbView));
  }

  @Override
  void disposeInternal() {
    checkValid(dbView);
    nativeFree(getNativeHandle());
  }

  private static native long nativeCreate(long viewNativeHandle, byte[] prefix);

  private native void nativePut(byte[] key, byte[] value, long nativeHandle);

  private native byte[] nativeGet(byte[] key, long nativeHandle);

  private native void nativeRemove(byte[] key, long nativeHandle);

  private native long nativeCreateKeysIter(long nativeHandle);

  private native byte[] nativeKeysIterNext(long iterNativeHandle);

  private native void nativeKeysIterFree(long iterNativeHandle);

  private native long nativeCreateValuesIter(long nativeHandle);

  private native byte[] nativeValuesIterNext(long iterNativeHandle);

  private native void nativeValuesIterFree(long iterNativeHandle);

  private native void nativeClear(long nativeHandle);

  private native void nativeFree(long nativeHandle);

}
