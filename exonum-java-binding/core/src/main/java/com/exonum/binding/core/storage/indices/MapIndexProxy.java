/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.core.storage.indices;

import static com.exonum.binding.core.storage.indices.StoragePreconditions.checkIdInGroup;
import static com.exonum.binding.core.storage.indices.StoragePreconditions.checkIndexName;

import com.exonum.binding.common.collect.MapEntry;
import com.exonum.binding.common.serialization.CheckingSerializerDecorator;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.proxy.ProxyDestructor;
import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.core.util.LibraryLoader;
import com.google.protobuf.MessageLite;
import java.util.Iterator;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * A MapIndex is an index that maps keys to values. A map cannot contain duplicate keys;
 * each key corresponds to at most one value.
 *
 * <p>The map implementation does not permit null keys and values.
 *
 * <p>The "destructive" methods of the map, i.e., the one that change the map contents,
 * are specified to throw {@link UnsupportedOperationException} if
 * the map has been created with a read-only database view.
 *
 * <p>All method arguments are non-null by default.
 *
 * <p>This class is not thread-safe and its instances shall not be shared between threads.
 *
 * <p>When the view goes out of scope, this map is destroyed. Subsequent use of the closed map
 * is prohibited and will result in {@link IllegalStateException}.
 *
 * @param <K> the type of keys in this map
 * @param <V> the type of values in this map
 * @see View
 */
public final class MapIndexProxy<K, V> extends AbstractIndexProxy implements MapIndex<K, V> {

  static {
    LibraryLoader.load();
  }

  private final CheckingSerializerDecorator<K> keySerializer;
  private final CheckingSerializerDecorator<V> valueSerializer;

  /**
   * Creates a new MapIndexProxy using protobuf messages.
   *
   * <p>If only a key or a value is a protobuf message, use
   * {@link MapIndexProxy#newInstance(String, View, Serializer, Serializer)}
   * and {@link com.exonum.binding.common.serialization.StandardSerializers#protobuf(Class)}.
   *
   * @param name a unique alphanumeric non-empty identifier of this map in the underlying storage:
   *             [a-zA-Z0-9_]
   * @param view a database view. Must be valid
   *             If a view is read-only, "destructive" operations are not permitted
   * @param keyType the class of keys-protobuf messages
   * @param valueType the class of values-protobuf messages
   * @param <K> the type of keys in the map; must be a protobuf message
   * @param <V> the type of values in the map; must be a protobuf message
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the name is empty; or a key or value class is
   *     not a valid protobuf message that has a public static {@code #parseFrom(byte[])} method
   */
  public static <K extends MessageLite, V extends MessageLite> MapIndexProxy<K, V> newInstance(
      String name, View view, Class<K> keyType, Class<V> valueType) {
    return newInstance(name, view, StandardSerializers.protobuf(keyType),
        StandardSerializers.protobuf(valueType));
  }

  /**
   * Creates a new MapIndexProxy.
   *
   * @param name a unique alphanumeric non-empty identifier of this map in the underlying storage:
   *             [a-zA-Z0-9_]
   * @param view a database view. Must be valid.
   *             If a view is read-only, "destructive" operations are not permitted.
   * @param keySerializer a serializer of keys
   * @param valueSerializer a serializer of values
   * @param <K> the type of keys in the map
   * @param <V> the type of values in the map
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the name is empty
   * @see StandardSerializers
   */
  public static <K, V> MapIndexProxy<K, V> newInstance(String name, View view,
                                                       Serializer<K> keySerializer,
                                                       Serializer<V> valueSerializer) {
    checkIndexName(name);
    CheckingSerializerDecorator<K> ks = CheckingSerializerDecorator.from(keySerializer);
    CheckingSerializerDecorator<V> vs = CheckingSerializerDecorator.from(valueSerializer);

    long viewNativeHandle = view.getViewNativeHandle();
    NativeHandle mapNativeHandle = createNativeMap(view,
        () -> nativeCreate(name, viewNativeHandle));

    return new MapIndexProxy<>(mapNativeHandle, name, view, ks, vs);
  }

  /**
   * Creates a new map in a <a href="package-summary.html#families">collection group</a>
   * with the given name.
   *
   * <p>See a <a href="package-summary.html#families-limitations">caveat</a> on index identifiers.
   *
   * @param groupName a name of the collection group
   * @param mapId an identifier of this collection in the group, see the caveats
   * @param view a database view
   * @param keySerializer a serializer of keys
   * @param valueSerializer a serializer of values
   * @param <K> the type of keys in the map
   * @param <V> the type of values in the map
   * @return a new map proxy
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the name or index id is empty
   * @see StandardSerializers
   */
  public static <K, V> MapIndexProxy<K, V> newInGroupUnsafe(String groupName,
                                                            byte[] mapId,
                                                            View view,
                                                            Serializer<K> keySerializer,
                                                            Serializer<V> valueSerializer) {
    checkIndexName(groupName);
    checkIdInGroup(mapId);
    CheckingSerializerDecorator<K> ks = CheckingSerializerDecorator.from(keySerializer);
    CheckingSerializerDecorator<V> vs = CheckingSerializerDecorator.from(valueSerializer);

    long viewNativeHandle = view.getViewNativeHandle();
    NativeHandle mapNativeHandle = createNativeMap(view,
        () -> nativeCreateInGroup(groupName, mapId, viewNativeHandle));

    return new MapIndexProxy<>(mapNativeHandle, groupName, view, ks, vs);
  }

  private static NativeHandle createNativeMap(View view, LongSupplier nativeMapConstructor) {
    NativeHandle mapNativeHandle = new NativeHandle(nativeMapConstructor.getAsLong());

    Cleaner cleaner = view.getCleaner();
    ProxyDestructor.newRegistered(cleaner, mapNativeHandle, MapIndexProxy.class,
        MapIndexProxy::nativeFree);
    return mapNativeHandle;
  }

  private MapIndexProxy(NativeHandle nativeHandle, String name, View view,
                        CheckingSerializerDecorator<K> keySerializer,
                        CheckingSerializerDecorator<V> valueSerializer) {
    super(nativeHandle, name, view);
    this.keySerializer = keySerializer;
    this.valueSerializer = valueSerializer;
  }

  @Override
  public boolean containsKey(K key) {
    byte[] dbKey = keySerializer.toBytes(key);
    return nativeContainsKey(getNativeHandle(), dbKey);
  }

  @Override
  public void put(K key, V value) {
    notifyModified();
    putInternal(getNativeHandle(), key, value);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> sourceMap) {
    notifyModified();
    long nativeHandle = getNativeHandle();
    for (Map.Entry<? extends K, ? extends V> entry : sourceMap.entrySet()) {
      putInternal(nativeHandle, entry.getKey(), entry.getValue());
    }
  }

  private void putInternal(long thisNativeHandle, K key, V value) {
    byte[] dbKey = keySerializer.toBytes(key);
    byte[] dbValue = valueSerializer.toBytes(value);
    nativePut(thisNativeHandle, dbKey, dbValue);
  }

  @Override
  public V get(K key) {
    byte[] dbKey = keySerializer.toBytes(key);
    byte[] dbValue = nativeGet(getNativeHandle(), dbKey);
    return (dbValue == null) ? null : valueSerializer.fromBytes(dbValue);
  }

  @Override
  public void remove(K key) {
    notifyModified();
    byte[] dbKey = keySerializer.toBytes(key);
    nativeRemove(getNativeHandle(), dbKey);
  }

  @Override
  public Iterator<K> keys() {
    return StorageIterators.createIterator(
        nativeCreateKeysIter(getNativeHandle()),
        this::nativeKeysIterNext,
        this::nativeKeysIterFree,
        dbView,
        modCounter,
        keySerializer::fromBytes
    );
  }

  @Override
  public Iterator<V> values() {
    return StorageIterators.createIterator(
        nativeCreateValuesIter(getNativeHandle()),
        this::nativeValuesIterNext,
        this::nativeValuesIterFree,
        dbView,
        modCounter,
        valueSerializer::fromBytes
    );
  }

  @Override
  public Iterator<MapEntry<K, V>> entries() {
    return StorageIterators.createIterator(
        nativeCreateEntriesIter(getNativeHandle()),
        this::nativeEntriesIterNext,
        this::nativeEntriesIterFree,
        dbView,
        modCounter,
        (entry) -> entry.toMapEntry(entry, keySerializer, valueSerializer)
    );
  }

  private native long nativeCreateEntriesIter(long nativeHandle);

  private native MapEntryInternal nativeEntriesIterNext(long iterNativeHandle);

  private native void nativeEntriesIterFree(long iterNativeHandle);

  @Override
  public void clear() {
    notifyModified();
    nativeClear(getNativeHandle());
  }

  private static native long nativeCreate(String name, long viewNativeHandle);

  private static native long nativeCreateInGroup(String groupName, byte[] mapId,
                                                 long viewNativeHandle);

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

  private static native void nativeFree(long nativeHandle);

}
