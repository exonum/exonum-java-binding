package com.exonum.binding.storage.serialization;

import com.exonum.binding.storage.indices.EntryIndexProxy;

/**
 * Converts Java objects into a binary representation in some format, and vice versa.
 *
 * <p>Implementations <strong>must</strong> ensure that for any object o,
 * {@link #toBytes(Object)} produces such an array, that being passed to {@link #fromBytes(byte[])},
 * is converted to another object o2, that is equal to the original object o.
 *
 * <p>This interface is designed to be primarily used by storage proxies
 * (e.g., {@linkplain EntryIndexProxy}).
 *
 * @param <T> a type of serializable object
 */
public interface Serializer<T> {

  /**
   * Serializes a given value into a byte array.
   *
   * @param value a value to serialize, must not be null
   * @return a byte array containing a serialized value
   * @throws NullPointerException if value is null
   */
  byte[] toBytes(T value);

  /**
   * De-serializes a value from a given byte array.
   *
   * @param serializedValue an array containing a serialized value of type T, must not be null
   * @return a value
   * @throws NullPointerException if array is null
   * @throws IllegalArgumentException if array cannot be de-serialized into a value of type T
   */
  T fromBytes(byte[] serializedValue);
}
