package com.exonum.binding.index;

import com.exonum.binding.storage.connector.Connect;
import com.exonum.binding.storage.serialization.RawValue;
import com.exonum.binding.storage.serialization.StorageKey;
import com.exonum.binding.storage.serialization.StorageValue;

public class IndexMap<K extends StorageKey, V extends StorageValue> {

  private final Class<? extends StorageValue> valueClass;
  private final Connect dbConnect;
  private long nativeIndexMap;

  public IndexMap(Class<? extends StorageValue> valueClass, Connect connect, byte[] prefix) {
    this.valueClass = valueClass;
    this.dbConnect = connect;
    this.nativeIndexMap = createNativeIndexMap(connect, prefix);
  }

  public void put(K key, V value) {
    dbConnect.lockWrite();
    try {
      putToIndexMap(key.serializeToRaw().getRaw(), value.serializeToRaw().getRaw(), nativeIndexMap);
    } finally {
      dbConnect.unlockWrite();
    }
  }

  public V get(K key) {
    dbConnect.lockRead();
    RawValue rawWrapper = null;
    try {
      rawWrapper = new RawValue(getFromIndexMap(key.serializeToRaw().getRaw(), nativeIndexMap));
    } finally {
      dbConnect.unlockRead();
    }

    // temporary deserialization decision
    V tmp = null;
    try {
      tmp = (V) valueClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      e.printStackTrace();
    }

    tmp.deserializeFromRaw(rawWrapper.getRaw());

    return tmp;
  }

  public void delete(K key) {

    dbConnect.lockRead();
    try {
      deleteFromIndexMap(key.serializeToRaw().getRaw(), nativeIndexMap);
    } finally {
      dbConnect.unlockRead();
    }
  }

  private native boolean putToIndexMap(byte[] key, byte[] value, Object nativeIndex);

  private native byte[] getFromIndexMap(byte[] key, Object nativeIndex);

  private native boolean deleteFromIndexMap(byte[] key, Object nativeIndex);

  private native long createNativeIndexMap(Connect connect, byte[] prefix);
}
