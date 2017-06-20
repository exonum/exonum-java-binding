package com.exonum.binding.index;

import com.exonum.binding.storage.connector.Connect;

public class IndexMap {
  private final Connect dbConnect;
  private long nativeIndexMap;

  public IndexMap(Connect connect, byte[] prefix) {
    this.dbConnect = connect;
    this.nativeIndexMap = nativeCreate(connect.getNativeHandle(), prefix);
  }

  public void put(byte[] key, byte[] value) {
    dbConnect.lockWrite();
    try {
      nativePut(key, value, nativeIndexMap);
    } finally {
      dbConnect.unlockWrite();
    }
  }

  public byte[] get(byte[] key) {
    dbConnect.lockRead();
    try {
      return nativeGet(key, nativeIndexMap);
    } finally {
      dbConnect.unlockRead();
    }
  }

  public void delete(byte[] key) {
    dbConnect.lockWrite();
    try {
      nativeDelete(key, nativeIndexMap);
    } finally {
      dbConnect.unlockWrite();
    }
  }

  private native long nativeCreate(long viewNativeHandle, byte[] prefix);

  private native void nativePut(byte[] key, byte[] value, long nativeIndex);

  private native byte[] nativeGet(byte[] key, long nativeIndex);

  private native void nativeDelete(byte[] key, long nativeIndex);
}
