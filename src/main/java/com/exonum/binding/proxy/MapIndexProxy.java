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

  /**
   * Returns true if this map contains a mapping for the specified key.
   *
   * @throws NullPointerException if the key is null
   * @throws IllegalStateException if this map is not valid
   */
  public boolean containsKey(byte[] key) {
    return nativeContainsKey(getNativeHandle(), checkStorageKey(key));
  }

  public void put(byte[] key, byte[] value) {
    notifyModified();
    nativePut(getNativeHandle(), checkStorageKey(key), checkStorageValue(value));
  }

  public byte[] get(byte[] key) {
    return nativeGet(getNativeHandle(), checkStorageKey(key));
  }

  public void remove(byte[] key) {
    notifyModified();
    nativeRemove(getNativeHandle(), checkStorageKey(key));
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

  private native boolean nativeContainsKey(long nativeHandle, byte[] key);

  private native void nativePut(long nativeHandle, byte[] key, byte[] value);

  private native byte[] nativeGet(long nativeHandle, byte[] key);

  private native void nativeRemove(long nativeHandle, byte[] key);

  private native long nativeCreateKeysIter(long nativeHandle);

  private native byte[] nativeKeysIterNext(long iterNativeHandle);

  private native void nativeKeysIterFree(long iterNativeHandle);

  private native long nativeCreateValuesIter(long nativeHandle);

  private native byte[] nativeValuesIterNext(long iterNativeHandle);

  private native void nativeValuesIterFree(long iterNativeHandle);

  private native void nativeClear(long nativeHandle);

  private native void nativeFree(long nativeHandle);

}
