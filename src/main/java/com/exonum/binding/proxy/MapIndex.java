package com.exonum.binding.proxy;

/**
 * A MapIndex is an index that maps keys to values. A map cannot contain duplicate keys;
 * each key corresponds to at most one value.
 *
 * <p>The "destructive" methods of the map, i.e., the one that change the map contents,
 * are specified to throw {@link UnsupportedOperationException} if
 * the map has been created with a read-only database view.
 *
 * <p>This interface prohibits null keys and values.
 */
public interface MapIndex extends NativeProxy {

  /**
   * Returns true if this map contains a mapping for the specified key.
   *
   * @throws NullPointerException if the key is null
   * @throws IllegalStateException if this map is not valid
   */
  boolean containsKey(byte[] key);

  /**
   * Puts a new key-value pair into the map. If this map already contains
   * a mapping for the specified key, overwrites the old value with the specified value.
   *
   * @param key a storage key
   * @param value a storage value to associate with the key
   * @throws NullPointerException if any argument is null
   * @throws IllegalStateException if this map is not valid
   * @throws IllegalArgumentException if some property of the key or the value prevents it
   *                                  from being stored in this map
   * @throws UnsupportedOperationException if this map is read-only
   */
  void put(byte[] key, byte[] value);

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
  byte[] get(byte[] key);

  /**
   * Removes the value mapped to the specified key from the map.
   * If there is no such mapping, has no effect.
   *
   * @param key a storage key
   * @throws NullPointerException if the key is null
   * @throws IllegalStateException if this map is not valid
   * @throws UnsupportedOperationException if this map is read-only
   */
  void remove(byte[] key);
  
  /**
   * Returns an iterator over the map keys in lexicographical order.
   *
   * <p>The iterator must be explicitly closed.
   *
   * <p>Any destructive operation on the same {@link Fork} this map uses
   * (but not necessarily on <em>this map</em>) will invalidate the iterator.
   *
   * @throws IllegalStateException if this map is not valid
   */
  // TODO(dt): consider creating a subclass (RustByteIter) so that you don't have to put a
  // type parameter?
  StorageIterator<byte[]> keys();

  /**
   * Returns an iterator over the map values in lexicographical order of <em>keys</em>.
   *
   * <p>The iterator must be explicitly closed.
   *
   * <p>Any destructive operation on the same {@link Fork} this map uses
   * (but not necessarily on <em>this map</em>) will invalidate the iterator.
   *
   * @throws IllegalStateException if this map is not valid
   */
  StorageIterator<byte[]> values();

  /**
   * Returns an iterator over the map entries.
   * The entries are ordered by keys in lexicographical order.
   *
   * <p>The iterator must be explicitly closed.
   *
   * <p>Any destructive operation on the same {@link Fork} this map uses
   * (but not necessarily on <em>this map</em>) will invalidate the iterator.
   *
   * @throws IllegalStateException if this map is not valid
   */
  StorageIterator<MapEntry> entries();

  /**
   * Removes all of the key-value pairs from the map.
   * The map will be empty after this method returns.
   *
   * @throws IllegalStateException if this map is not valid
   * @throws UnsupportedOperationException if this map is read-only
   */
  void clear();
}
