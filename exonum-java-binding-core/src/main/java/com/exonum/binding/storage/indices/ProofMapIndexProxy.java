/*
 * Copyright 2018 The Exonum Team
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

package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.StoragePreconditions.checkIdInGroup;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkIndexName;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.proofs.map.flat.UncheckedMapProof;
import com.exonum.binding.common.serialization.CheckingSerializerDecorator;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.proxy.ProxyDestructor;
import com.exonum.binding.storage.database.View;
import java.util.Iterator;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * A ProofMapIndexProxy is an index that maps keys to values. A map cannot contain duplicate keys;
 * each key corresponds to at most one value. This map is capable of providing cryptographic proofs
 * that a certain key is mapped to a particular value <em>or</em> that there are no mapping for
 * the key in the map.
 *
 * <p>This map is implemented as a Merkle-Patricia tree. It does not permit null keys and values,
 * and requires that keys are 32-byte long.
 *
 * <p>The "destructive" methods of the map, i.e., the one that change the map contents,
 * are specified to throw {@link UnsupportedOperationException} if
 * the map has been created with a read-only database view.
 *
 * <p>All method arguments are non-null by default.
 *
 * <p>This class is not thread-safe and and its instances shall not be shared between threads.
 *
 * <p>When the view goes out of scope, this map is destroyed. Subsequent use of the closed map
 * is prohibited and will result in {@link IllegalStateException}.
 *
 * @param <K> the type of keys in this map. Must be 32-byte long in the serialized form
 * @param <V> the type of values in this map
 * @see View
 */
public final class ProofMapIndexProxy<K, V> extends AbstractIndexProxy implements MapIndex<K, V> {

  private final ProofMapKeyCheckingSerializerDecorator<K> keySerializer;
  private final CheckingSerializerDecorator<V> valueSerializer;

  /**
   * Creates a ProofMapIndexProxy.
   *
   * @param name a unique alphanumeric non-empty identifier of this map in the underlying storage:
   *             [a-zA-Z0-9_]
   * @param view a database view. Must be valid.
   *             If a view is read-only, "destructive" operations are not permitted.
   * @param keySerializer a serializer of keys, must always produce 32-byte long values
   * @param valueSerializer a serializer of values
   * @param <K> the type of keys in the map
   * @param <V> the type of values in the map
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the name is empty
   * @see StandardSerializers
   */
  public static <K, V> ProofMapIndexProxy<K, V> newInstance(
      String name, View view, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
    checkIndexName(name);
    ProofMapKeyCheckingSerializerDecorator<K> ks =
        ProofMapKeyCheckingSerializerDecorator.from(keySerializer);
    CheckingSerializerDecorator<V> vs = CheckingSerializerDecorator.from(valueSerializer);

    long viewNativeHandle = view.getViewNativeHandle();
    NativeHandle mapNativeHandle = createNativeMap(view,
        () -> nativeCreate(name, viewNativeHandle));

    return new ProofMapIndexProxy<>(mapNativeHandle, name, view, ks, vs);
  }

  /**
   * Creates a new proof map in a <a href="package-summary.html#families">collection group</a>
   * with the given name.
   *
   * <p>See a <a href="package-summary.html#families-limitations">caveat</a> on index identifiers.
   *
   * @param groupName a name of the collection group
   * @param mapId an identifier of this collection in the group, see the caveats
   * @param view a database view
   * @param keySerializer a serializer of keys, must always produce 32-byte long values
   * @param valueSerializer a serializer of values
   * @param <K> the type of keys in the map
   * @param <V> the type of values in the map
   * @return a new map proxy
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the name or index id is empty
   * @see StandardSerializers
   */
  public static <K, V> ProofMapIndexProxy<K, V> newInGroupUnsafe(String groupName,
                                                                 byte[] mapId,
                                                                 View view,
                                                                 Serializer<K> keySerializer,
                                                                 Serializer<V> valueSerializer) {
    checkIndexName(groupName);
    checkIdInGroup(mapId);
    ProofMapKeyCheckingSerializerDecorator<K> ks =
        ProofMapKeyCheckingSerializerDecorator.from(keySerializer);
    CheckingSerializerDecorator<V> vs = CheckingSerializerDecorator.from(valueSerializer);

    long viewNativeHandle = view.getViewNativeHandle();
    NativeHandle mapNativeHandle = createNativeMap(view,
        () -> nativeCreateInGroup(groupName, mapId, viewNativeHandle));

    return new ProofMapIndexProxy<>(mapNativeHandle, groupName, view, ks, vs);
  }

  private static NativeHandle createNativeMap(View view, LongSupplier nativeMapConstructor) {
    NativeHandle mapNativeHandle = new NativeHandle(nativeMapConstructor.getAsLong());

    Cleaner cleaner = view.getCleaner();
    ProxyDestructor.newRegistered(cleaner, mapNativeHandle, ProofMapIndexProxy.class,
        ProofMapIndexProxy::nativeFree);
    return mapNativeHandle;
  }

  private static native long nativeCreate(String name, long viewNativeHandle);

  private static native long nativeCreateInGroup(String groupName, byte[] mapId,
                                                 long viewNativeHandle);

  private ProofMapIndexProxy(NativeHandle nativeHandle, String name, View view,
                             ProofMapKeyCheckingSerializerDecorator<K> keySerializer,
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

  private native boolean nativeContainsKey(long nativeHandle, byte[] key);

  /**
   * {@inheritDoc}
   *
   * @param key a proof map key, must be 32-byte long when serialized
   * @param value a storage value to associate with the key
   * @throws IllegalStateException if this map is not valid
   * @throws IllegalArgumentException if the size of the key is not 32 bytes
   * @throws UnsupportedOperationException if this map is read-only
   */
  @Override
  public void put(K key, V value) {
    notifyModified();
    long nativeHandle = getNativeHandle();
    putInternal(nativeHandle, key, value);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> sourceMap) {
    notifyModified();
    long nativeHandle = getNativeHandle();
    for (Map.Entry<? extends K, ? extends V> entry : sourceMap.entrySet()) {
      putInternal(nativeHandle, entry.getKey(), entry.getValue());
    }
  }

  private void putInternal(long nativeHandle, K key, V value) {
    byte[] dbKey = keySerializer.toBytes(key);
    byte[] dbValue = valueSerializer.toBytes(value);
    nativePut(nativeHandle, dbKey, dbValue);
  }

  private native void nativePut(long nativeHandle, byte[] key, byte[] value);

  @Override
  public V get(K key) {
    byte[] dbKey = keySerializer.toBytes(key);
    byte[] dbValue = nativeGet(getNativeHandle(), dbKey);
    return (dbValue == null) ? null : valueSerializer.fromBytes(dbValue);
  }

  private native byte[] nativeGet(long nativeHandle, byte[] key);

  /**
   * Returns a proof that there is a value mapped to the specified key or
   * that there is no such mapping.
   *
   * @param key a proof map key which might be mapped to some value, must be 32-byte long
   * @throws IllegalStateException  if this map is not valid
   * @throws IllegalArgumentException if the size of the key is not 32 bytes
   */
  public UncheckedMapProof getProof(K key) {
    byte[] dbKey = keySerializer.toBytes(key);
    return nativeGetProof(getNativeHandle(), dbKey);
  }

  private native UncheckedMapProof nativeGetProof(long nativeHandle, byte[] key);

  /**
   * Returns the root hash of the underlying Merkle-Patricia tree.
   *
   * @throws IllegalStateException  if this map is not valid
   */
  public HashCode getRootHash() {
    return HashCode.fromBytes(nativeGetRootHash(getNativeHandle()));
  }

  private native byte[] nativeGetRootHash(long nativeHandle);

  @Override
  public void remove(K key) {
    notifyModified();
    byte[] dbKey = keySerializer.toBytes(key);
    nativeRemove(getNativeHandle(), dbKey);
  }

  private native void nativeRemove(long nativeHandle, byte[] key);

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

  private native long nativeCreateKeysIter(long nativeHandle);

  private native byte[] nativeKeysIterNext(long iterNativeHandle);

  private native void nativeKeysIterFree(long iterNativeHandle);

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

  private native long nativeCreateValuesIter(long nativeHandle);

  private native byte[] nativeValuesIterNext(long iterNativeHandle);

  private native void nativeValuesIterFree(long iterNativeHandle);

  @Override
  public Iterator<MapEntry<K, V>> entries() {
    return StorageIterators.createIterator(
        nativeCreateEntriesIter(getNativeHandle()),
        this::nativeEntriesIterNext,
        this::nativeEntriesIterFree,
        dbView,
        modCounter,
        (entry) -> MapEntry.fromInternal(entry, keySerializer, valueSerializer)
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

  private native void nativeClear(long nativeHandle);

  private static native void nativeFree(long nativeHandle);
}
