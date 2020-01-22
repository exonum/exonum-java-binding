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


import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.common.collect.MapEntry;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.CheckingSerializerDecorator;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.proxy.ProxyDestructor;
import com.exonum.binding.core.storage.database.AbstractAccess;
import com.exonum.binding.core.storage.database.Access;
import com.google.common.collect.Lists;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * A ProofMapIndexProxy is an index that maps keys to values. A map cannot contain duplicate keys;
 * each key corresponds to at most one value. This map is capable of providing cryptographic proofs
 * that a certain key is mapped to a particular value <em>or</em> that there are no mapping for
 * the key in the map.
 *
 * <p>This map is implemented as a Merkle-Patricia tree. It does not permit null keys and values.
 *
 * <p>The Merkle-Patricia tree backing the proof map uses internal 32-byte keys. The tree balance
 * relies on the internal keys being uniformly distributed.
 *
 * <h3 id="key-hashing">Key hashing in proof maps</h3>
 *
 * <!-- TODO: consider better place for this discussion -->
 * <p>By default, when creating the proof map using method
 * {@link #newInstance(IndexAddress, AbstractAccess, Serializer, Serializer) #newInstance},
 * the user keys are converted into internal keys through hashing. This allows to use keys of
 * an arbitrary size and ensures the balance of the internal tree.
 * It is also possible to create a proof map that will not hash keys with methods
 * {@link #newInstanceNoKeyHashing(IndexAddress, AbstractAccess, Serializer, Serializer)
 * #newInstanceNoKeyHashing}. In this mode the map will use the user keys as internal
 * tree keys. Such mode of operation is appropriate iff <em>all</em> of the following conditions
 * hold:
 *
 * <ul>
 *   <li>All keys are 32-byte long</li>
 *   <li>The keys are uniformly distributed</li>
 *   <li>The keys come from a trusted source that cannot influence their distribution and affect
 *       the tree balance.</li>
 * </ul>
 *
 * <hr>
 *
 * <p>The "destructive" methods of the map, i.e., the one that change the map contents,
 * are specified to throw {@link UnsupportedOperationException} if
 * the map has been created with a read-only database access.
 *
 * <p>All method arguments are non-null by default.
 *
 * <p>This class is not thread-safe and and its instances shall not be shared between threads.
 *
 * <p>When the access goes out of scope, this map is destroyed. Subsequent use of the closed map
 * is prohibited and will result in {@link IllegalStateException}.
 *
 * @param <K> the type of keys in this map
 * @param <V> the type of values in this map
 * @see Access
 */
public final class ProofMapIndexProxy<K, V> extends AbstractIndexProxy implements MapIndex<K, V>,
    HashableIndex {

  private final Serializer<K> keySerializer;
  private final CheckingSerializerDecorator<V> valueSerializer;

  /**
   * Creates a ProofMapIndexProxy.
   *
   * @param address an index address
   * @param access a database access. Must be valid.
   *             If an access is read-only, "destructive" operations are not permitted.
   * @param keySerializer a serializer of keys
   * @param valueSerializer a serializer of values
   * @param <K> the type of keys in the map
   * @param <V> the type of values in the map
   * @throws IllegalStateException if the access is not valid
   * @throws IllegalArgumentException if the name is empty
   * @see StandardSerializers
   */
  public static <K, V> ProofMapIndexProxy<K, V> newInstance(
      IndexAddress address, AbstractAccess access, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
    return newMapIndexProxy(address, access, keySerializer, valueSerializer, true);
  }

  /**
   * Creates a <a href="ProofMapIndexProxy.html#key-hashing">ProofMapIndexProxy that uses non-hashed keys</a>.
   * Requires that keys are 32-byte long.
   *
   * @param address an index address
   * @param access a database access. Must be valid.
   *             If an access is read-only, "destructive" operations are not permitted.
   * @param keySerializer a serializer of keys, must always produce 32-byte long values
   * @param valueSerializer a serializer of values
   * @param <K> the type of keys in the map
   * @param <V> the type of values in the map
   * @throws IllegalStateException if the access is not valid
   * @throws IllegalArgumentException if the name is empty
   * @see StandardSerializers
   */
  public static <K, V> ProofMapIndexProxy<K, V> newInstanceNoKeyHashing(
      IndexAddress address, AbstractAccess access, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
    return newMapIndexProxy(address, access, keySerializer, valueSerializer, false);
  }

  private static <K, V> ProofMapIndexProxy<K, V> newMapIndexProxy(IndexAddress address,
      AbstractAccess access,
      Serializer<K> keySerializer, Serializer<V> valueSerializer, boolean keyHashing) {
    Serializer<K> ks = decorateKeySerializer(keySerializer, keyHashing);
    CheckingSerializerDecorator<V> vs = CheckingSerializerDecorator.from(valueSerializer);

    NativeHandle mapNativeHandle = createNativeMap(address, access, keyHashing);

    return new ProofMapIndexProxy<>(mapNativeHandle, address,
        access, ks, vs);
  }

  private static <K> Serializer<K> decorateKeySerializer(
      Serializer<K> keySerializer, boolean keyHashing) {
    if (!keyHashing) {
      return ProofMapKeyCheckingSerializerDecorator.from(keySerializer);
    } else {
      return CheckingSerializerDecorator.from(keySerializer);
    }
  }

  private static NativeHandle createNativeMap(IndexAddress address, AbstractAccess access,
      boolean keyHashing) {
    long accessNativeHandle = access.getAccessNativeHandle();
    long handle = nativeCreate(address.getName(), address.getIdInGroup().orElse(null),
        accessNativeHandle, keyHashing);
    NativeHandle mapNativeHandle = new NativeHandle(handle);

    Cleaner cleaner = access.getCleaner();
    ProxyDestructor.newRegistered(cleaner, mapNativeHandle, ProofMapIndexProxy.class,
        ProofMapIndexProxy::nativeFree);
    return mapNativeHandle;
  }

  private static native long nativeCreate(String name, @Nullable byte[] idInGroup,
      long accessNativeHandle, boolean keyHashing);

  private ProofMapIndexProxy(NativeHandle nativeHandle, IndexAddress address, AbstractAccess access,
                             Serializer<K> keySerializer,
                             CheckingSerializerDecorator<V> valueSerializer) {
    super(nativeHandle, address, access);
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
   * @param key a proof map key
   * @param value a storage value to associate with the key
   * @throws IllegalStateException if this map is not valid
   * @throws IllegalArgumentException if the size of the key is not 32 bytes (in case of a
   *     <a href="ProofMapIndexProxy.html#key-hashing">proof map that uses non-hashed keys</a>)
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
   * Returns a proof that there are values mapped to the specified keys or that there are no such
   * mappings.
   *
   * @param key a proof map key which might be mapped to some value
   * @param otherKeys other proof map keys which might be mapped to some values
   * @throws IllegalStateException if this map is not valid
   * @throws IllegalArgumentException if the size of any of the keys is not 32 bytes (in case of a
   *     <a href="ProofMapIndexProxy.html#key-hashing">proof map that uses non-hashed keys</a>)
   * @see <a href="../../blockchain/Blockchain.html#proofs">Blockchain Proofs</a>
   */
  public MapProof getProof(K key, K... otherKeys) {
    if (otherKeys.length == 0) {
      return getSingleKeyProof(key);
    } else {
      List<K> keys = Lists.asList(key, otherKeys);
      return getMultiKeyProof(keys);
    }
  }

  /**
   * Returns a proof that there are values mapped to the specified keys or that there are no such
   * mappings.
   *
   * @param keys proof map keys which might be mapped to some values
   * @throws IllegalStateException if this map is not valid
   * @throws IllegalArgumentException if the size of any of the keys is not 32 bytes (in case of a
   *     <a href="ProofMapIndexProxy.html#key-hashing">proof map that uses non-hashed keys</a>) or
   *     keys collection is empty
   * @see <a href="../../blockchain/Blockchain.html#proofs">Blockchain Proofs</a>
   */
  public MapProof getProof(Collection<? extends K> keys) {
    checkArgument(!keys.isEmpty(), "Keys collection should not be empty");
    if (keys.size() == 1) {
      K key = keys.iterator()
          .next();
      return getSingleKeyProof(key);
    } else {
      return getMultiKeyProof(keys);
    }
  }

  private MapProof getSingleKeyProof(K key) {
    byte[] dbKey = keySerializer.toBytes(key);
    byte[] proofMessage = nativeGetProof(getNativeHandle(), dbKey);
    return decodeProofMessage(proofMessage);
  }

  private native byte[] nativeGetProof(long nativeHandle, byte[] key);

  private MapProof getMultiKeyProof(Collection<? extends K> keys) {
    byte[][] dbKeys = keysToArray(keys);
    byte[] proofMessage = nativeGetMultiProof(getNativeHandle(), dbKeys);
    return decodeProofMessage(proofMessage);
  }

  private byte[][] keysToArray(Collection<? extends K> keys) {
    return keys.stream()
        .map(keySerializer::toBytes)
        .toArray(byte[][]::new);
  }

  private native byte[] nativeGetMultiProof(long nativeHandle, byte[][] keys);

  private static MapProof decodeProofMessage(byte[] proofMessage) {
    try {
      return MapProof.parseFrom(proofMessage);
    } catch (InvalidProtocolBufferException e) {
      // Must never happen with correct native code
      throw new IllegalStateException("Non-decodable proof message", e);
    }
  }

  @Override
  public HashCode getIndexHash() {
    return HashCode.fromBytes(nativeGetIndexHash(getNativeHandle()));
  }

  private native byte[] nativeGetIndexHash(long nativeHandle);

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
        dbAccess,
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
        dbAccess,
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
        dbAccess,
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

  private native void nativeClear(long nativeHandle);

  private static native void nativeFree(long nativeHandle);
}
