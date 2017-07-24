package com.exonum.binding.proxy;

import static com.exonum.binding.proxy.StoragePreconditions.checkCanModify;
import static com.exonum.binding.proxy.StoragePreconditions.checkIndexPrefix;
import static com.exonum.binding.proxy.StoragePreconditions.checkStorageKey;
import static com.exonum.binding.proxy.StoragePreconditions.checkStorageValue;
import static com.exonum.binding.proxy.StoragePreconditions.checkValid;

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
public class MapIndexProxy extends AbstractNativeProxy {
  // TODO: consider moving 'dbView' to a super class as 'parents'
  //       (= objects that must not be deleted before this)
  private final View dbView;

  private final ViewModificationCounter modCounter;

  /**
   * Creates a new MapIndexProxy.
   *
   * @param prefix a unique identifier of this map in the underlying storage
   * @param view a database view. Must be valid.
   *             If a view is read-only, "destructive" operations are not permitted.
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the prefix has zero size
   * @throws NullPointerException if any argument is null
   */
  public MapIndexProxy(byte[] prefix, View view) {
    super(nativeCreate(checkIndexPrefix(prefix), view.getNativeHandle()),
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

  /**
   * Puts a new key-value pair into the map. If this map already contains
   * a mapping for the specified key, overwrites the old value with the specified value.
   *
   * @param key a storage key
   * @param value a storage value to associate with the key
   * @throws NullPointerException if any argument is null
   * @throws IllegalStateException if this map is not valid
   * @throws UnsupportedOperationException if this map is read-only
   */
  public void put(byte[] key, byte[] value) {
    notifyModified();
    nativePut(getNativeHandle(), checkStorageKey(key), checkStorageValue(value));
  }

  /**
   * Returns the value associated with the specified key,
   * or {@code null} if there is no mapping for the key.
   *
   * @param key a storage key
   * @return the value mapped to the specified key,
   *         or {@code null} if this map contains no mapping for the key.
   * @throws NullPointerException if the key is null
   * @throws IllegalStateException if this map is not valid
   */
  public byte[] get(byte[] key) {
    return nativeGet(getNativeHandle(), checkStorageKey(key));
  }

  /**
   * Removes the value mapped to the specified key from the map.
   * If there is no such mapping, has no effect.
   *
   * @param key a storage key
   * @throws NullPointerException if the key is null
   * @throws IllegalStateException if this map is not valid
   * @throws UnsupportedOperationException if this map is read-only
   */
  public void remove(byte[] key) {
    notifyModified();
    nativeRemove(getNativeHandle(), checkStorageKey(key));
  }

  /**
   * Returns an iterator over the map keys in lexicographical order.
   * Must be explicitly closed.
   *
   * <p>Any destructive operation on the same {@link Fork} this map uses
   * (but not necessarily on <em>this map</em>) will invalidate the iterator.
   *
   * @throws IllegalStateException if this map is not valid
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
   * Returns an iterator over the map values.
   * The values are ordered by <em>keys</em> in lexicographical order.
   * Must be explicitly closed.
   *
   * <p>Any destructive operation on the same {@link Fork} this map uses
   * (but not necessarily on <em>this map</em>) will invalidate the iterator.
   *
   * @throws IllegalStateException if this map is not valid
   */
  public RustIter<byte[]> values() {
    return new ConfigurableRustIter<>(nativeCreateValuesIter(getNativeHandle()),
          this::nativeValuesIterNext,
          this::nativeValuesIterFree,
          dbView,
          modCounter);
  }

  /**
   * Returns an iterator over the map entries.
   * The entries are ordered by keys in lexicographical order.
   * Must be explicitly closed.
   *
   * <p>Any destructive operation on the same {@link Fork} this map uses
   * (but not necessarily on <em>this map</em>) will invalidate the iterator.
   *
   * @throws IllegalStateException if this map is not valid
   */
  public RustIter<MapEntry> entries() {
    return new ConfigurableRustIter<>(nativeCreateEntriesIter(getNativeHandle()),
        this::nativeEntriesIterNext,
        this::nativeEntriesIterFree,
        dbView,
        modCounter);
  }

  private native long nativeCreateEntriesIter(long nativeHandle);

  private native MapEntry nativeEntriesIterNext(long iterNativeHandle);

  private native void nativeEntriesIterFree(long iterNativeHandle);

  /**
   * Removes all of the key-value pairs from the map.
   * The map will be empty after this method returns.
   *
   * @throws IllegalStateException if this map is not valid
   * @throws UnsupportedOperationException if this map is read-only
   */
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

  private static native long nativeCreate(byte[] prefix, long viewNativeHandle);

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
