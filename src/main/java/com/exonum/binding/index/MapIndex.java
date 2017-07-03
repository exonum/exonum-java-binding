package com.exonum.binding.index;

import com.exonum.binding.storage.connector.Connect;

public class MapIndex {
  private final Connect dbConnect;
  private long nativeIndexMap;

  public MapIndex(Connect connect, byte[] prefix) {
    this.dbConnect = connect;
    this.nativeIndexMap = nativeCreate(connect.getNativeHandle(), prefix);
  }

  public void put(byte[] key, byte[] value) {
    nativePut(key, value, nativeIndexMap);
  }

  public byte[] get(byte[] key) {
    return nativeGet(key, nativeIndexMap);
  }

  public void delete(byte[] key) {
    nativeDelete(key, nativeIndexMap);
  }

  private native long nativeCreate(long viewNativeHandle, byte[] prefix);

  private native void nativePut(byte[] key, byte[] value, long nativeIndex);

  private native byte[] nativeGet(byte[] key, long nativeIndex);

  private native void nativeDelete(byte[] key, long nativeIndex);
}
