package com.exonum.binding.proxy;

import com.exonum.binding.annotations.ImproveDocs;

@ImproveDocs(
    assignee = "dt",
    reason = "consider using exonum::storage docs + java.util.Map as a reference"
)
public class MapIndexProxy extends AbstractNativeProxy {
  @SuppressWarnings("unused")  // will use the reference to check if it is valid.
  private final Connect dbConnect;

  public MapIndexProxy(Connect connect, byte[] prefix) {
    super(nativeCreate(connect.getNativeHandle(), prefix), true);
    this.dbConnect = connect;
  }

  public void put(byte[] key, byte[] value) {
    nativePut(key, value, nativeHandle);
  }

  public byte[] get(byte[] key) {
    return nativeGet(key, nativeHandle);
  }

  public void remove(byte[] key) {
    nativeRemove(key, nativeHandle);
  }

  public void clear() {
    nativeClear(nativeHandle);
  }

  @Override
  void disposeInternal() {
    nativeFree(nativeHandle);
  }

  private static native long nativeCreate(long viewNativeHandle, byte[] prefix);

  private native void nativePut(byte[] key, byte[] value, long nativeHandle);

  private native byte[] nativeGet(byte[] key, long nativeHandle);

  private native void nativeRemove(byte[] key, long nativeHandle);

  private native void nativeClear(long nativeHandle);

  private native void nativeFree(long nativeHandle);
}
