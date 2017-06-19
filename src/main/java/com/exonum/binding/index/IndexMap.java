package com.exonum.binding.index;

import com.exonum.binding.storage.connector.Connect;

public class IndexMap {
  private final Connect dbConnect;
  private long nativeIndexMap;

  public IndexMap(Connect connect, byte[] prefix) {
    this.dbConnect = connect;
    this.nativeIndexMap = createNativeIndexMap(connect.getNativeHandle(), prefix);
  }

  public void put(byte[] key, byte[] value) {
    dbConnect.lockWrite();
    try {
      putToIndexMap(key, value, nativeIndexMap);
    } finally {
      dbConnect.unlockWrite();
    }
  }

  public byte[] get(byte[] key) {
    dbConnect.lockRead();
    try {
      return getFromIndexMap(key, nativeIndexMap);
    } finally {
      dbConnect.unlockRead();
    }
  }

  public void delete(byte[] key) {
    dbConnect.lockWrite();
    try {
      deleteFromIndexMap(key, nativeIndexMap);
    } finally {
      dbConnect.unlockWrite();
    }
  }

  private native long createNativeIndexMap(long viewNativeHandle, byte[] prefix);

  private native void putToIndexMap(byte[] key, byte[] value, long nativeIndex);

  private native byte[] getFromIndexMap(byte[] key, long nativeIndex);

  private native void deleteFromIndexMap(byte[] key, long nativeIndex);
}
