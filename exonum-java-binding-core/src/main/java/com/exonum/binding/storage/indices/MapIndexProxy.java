package com.exonum.binding.storage.indices;

import static com.exonum.binding.proxy.ProxyPreconditions.checkValid;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkIndexName;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkStorageKey;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkStorageValue;

import com.exonum.binding.storage.database.View;

/**
 * A MapIndex is an index that maps keys to values. A map cannot contain duplicate keys;
 * each key corresponds to at most one value.
 *
 * <p>The "destructive" methods of the map, i.e., the one that change the map contents,
 * are specified to throw {@link UnsupportedOperationException} if
 * the map has been created with a read-only database view.
 *
 * <p>The map implementation does not permit null keys and values.
 *
 * <p>As any native proxy, the map <em>must be closed</em> when no longer needed.
 * Subsequent use of the closed map is prohibited and will result in {@link IllegalStateException}.
 */
public class MapIndexProxy extends AbstractIndexProxy implements MapIndex {

  /**
   * Creates a new MapIndexProxy.
   *
   * @param name a unique alphanumeric identifier of this map in the underlying storage:
   *             [a-zA-Z0-9_]
   * @param view a database view. Must be valid.
   *             If a view is read-only, "destructive" operations are not permitted.
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the name is empty
   * @throws NullPointerException if any argument is null
   */
  public MapIndexProxy(String name, View view) {
    super(nativeCreate(checkIndexName(name), view.getViewNativeHandle()), view);
  }

  @Override
  public boolean containsKey(byte[] key) {
    return nativeContainsKey(getNativeHandle(), checkStorageKey(key));
  }

  @Override
  public void put(byte[] key, byte[] value) {
    notifyModified();
    nativePut(getNativeHandle(), checkStorageKey(key), checkStorageValue(value));
  }

  @Override
  public byte[] get(byte[] key) {
    return nativeGet(getNativeHandle(), checkStorageKey(key));
  }

  @Override
  public void remove(byte[] key) {
    notifyModified();
    nativeRemove(getNativeHandle(), checkStorageKey(key));
  }

  @Override
  public StorageIterator<byte[]> keys() {
    return StorageIterators.createIterator(
        nativeCreateKeysIter(getNativeHandle()),
        this::nativeKeysIterNext,
        this::nativeKeysIterFree,
        dbView,
        modCounter);
  }

  @Override
  public StorageIterator<byte[]> values() {
    return StorageIterators.createIterator(
        nativeCreateValuesIter(getNativeHandle()),
        this::nativeValuesIterNext,
        this::nativeValuesIterFree,
        dbView,
        modCounter);
  }

  @Override
  public StorageIterator<MapEntry> entries() {
    return StorageIterators.createIterator(
        nativeCreateEntriesIter(getNativeHandle()),
        this::nativeEntriesIterNext,
        this::nativeEntriesIterFree,
        dbView,
        modCounter);
  }

  private native long nativeCreateEntriesIter(long nativeHandle);

  private native MapEntry nativeEntriesIterNext(long iterNativeHandle);

  private native void nativeEntriesIterFree(long iterNativeHandle);

  @Override
  public void clear() {
    notifyModified();
    nativeClear(getNativeHandle());
  }

  @Override
  protected void disposeInternal() {
    checkValid(dbView);
    nativeFree(getNativeHandle());
  }

  private static native long nativeCreate(String name, long viewNativeHandle);

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
