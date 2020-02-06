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

package com.exonum.binding.core.storage.database;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.core.proxy.AbstractNativeProxy;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.storage.indices.EntryIndex;
import com.exonum.binding.core.storage.indices.EntryIndexProxy;
import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.storage.indices.KeySetIndexProxy;
import com.exonum.binding.core.storage.indices.ListIndexProxy;
import com.exonum.binding.core.storage.indices.MapIndexProxy;
import com.exonum.binding.core.storage.indices.ProofEntryIndexProxy;
import com.exonum.binding.core.storage.indices.ProofListIndexProxy;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.core.storage.indices.StorageIndex;
import com.exonum.binding.core.storage.indices.ValueSetIndexProxy;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * Represents an access to the database.
 *
 * <p>There are two sub-types:
 * <ul>
 *   <li>A snapshot, which is a <em>read-only</em> and immutable access.</li>
 *   <li>A fork, which is a <em>read-write</em> access.</li>
 * </ul>
 *
 * @see Snapshot
 * @see Fork
 */
public abstract class AbstractAccess extends AbstractNativeProxy implements Access {

  private static final long UNKNOWN_INDEX_ID = 0L;
  private final OpenIndexRegistry indexRegistry;
  private final boolean canModify;

  /**
   * Create a new access proxy.
   *
   * @param nativeHandle a native handle: an implementation-specific reference to a native object
   * @param canModify if the access allows modifications
   */
  AbstractAccess(NativeHandle nativeHandle, boolean canModify) {
    this(nativeHandle, canModify, new OpenIndexRegistry());
  }

  /**
   * Create a new access proxy with the given index registry.
   *
   * @param nativeHandle a native handle: an implementation-specific reference to a native object
   * @param canModify if the access allows modifications
   * @param registry a pool of open indexes to use
   */
  AbstractAccess(NativeHandle nativeHandle, boolean canModify, OpenIndexRegistry registry) {
    super(nativeHandle);
    this.canModify = canModify;
    indexRegistry = registry;
  }

  @SuppressWarnings("unchecked") // The compiler is correct: the cache is not type-safe: ECR-3387
  @Override
  public <E> ProofListIndexProxy<E> getProofList(IndexAddress address, Serializer<E> serializer) {
    return findOrCreate(address, ProofListIndexProxy.class,
        () -> ProofListIndexProxy.newInstance(address, this, serializer));
  }

  @SuppressWarnings("unchecked") // The compiler is correct: the cache is not type-safe: ECR-3387
  @Override
  public <E> ListIndexProxy<E> getList(IndexAddress address, Serializer<E> serializer) {
    return findOrCreate(address, ListIndexProxy.class,
        () -> ListIndexProxy.newInstance(address, this, serializer));
  }

  @SuppressWarnings("unchecked") // The compiler is correct: the cache is not type-safe: ECR-3387
  @Override
  public <K, V> ProofMapIndexProxy<K, V> getProofMap(IndexAddress address,
      Serializer<K> keySerializer, Serializer<V> valueSerializer) {
    return findOrCreate(address, ProofMapIndexProxy.class,
        () -> ProofMapIndexProxy.newInstance(address, this, keySerializer,
            valueSerializer));
  }

  @SuppressWarnings("unchecked") // The compiler is correct: the cache is not type-safe: ECR-3387
  @Override
  public <K, V> ProofMapIndexProxy<K, V> getRawProofMap(IndexAddress address,
      Serializer<K> keySerializer, Serializer<V> valueSerializer) {
    return findOrCreate(address, ProofMapIndexProxy.class,
        () -> ProofMapIndexProxy.newInstanceNoKeyHashing(address, this, keySerializer,
            valueSerializer));
  }

  @SuppressWarnings("unchecked") // The compiler is correct: the cache is not type-safe: ECR-3387
  @Override
  public <K, V> MapIndexProxy<K, V> getMap(IndexAddress address, Serializer<K> keySerializer,
      Serializer<V> valueSerializer) {
    return findOrCreate(address, MapIndexProxy.class,
        () -> MapIndexProxy.newInstance(address, this, keySerializer, valueSerializer));
  }

  @SuppressWarnings("unchecked") // The compiler is correct: the cache is not type-safe: ECR-3387
  @Override
  public <E> KeySetIndexProxy<E> getKeySet(IndexAddress address, Serializer<E> serializer) {
    return findOrCreate(address, KeySetIndexProxy.class,
        () -> KeySetIndexProxy.newInstance(address, this, serializer));
  }

  @SuppressWarnings("unchecked") // The compiler is correct: the cache is not type-safe: ECR-3387
  @Override
  public <E> ValueSetIndexProxy<E> getValueSet(IndexAddress address, Serializer<E> serializer) {
    return findOrCreate(address, ValueSetIndexProxy.class,
        () -> ValueSetIndexProxy.newInstance(address, this, serializer));
  }

  @SuppressWarnings("unchecked") // The compiler is correct: the cache is not type-safe: ECR-3387
  @Override
  public <E> ProofEntryIndexProxy<E> getProofEntry(IndexAddress address, Serializer<E> serializer) {
    return findOrCreate(address, ProofEntryIndexProxy.class,
        () -> ProofEntryIndexProxy.newInstance(address, this, serializer));
  }

  @SuppressWarnings("unchecked") // The compiler is correct: the cache is not type-safe: ECR-3387
  @Override
  public <E> EntryIndex<E> getEntry(IndexAddress address, Serializer<E> serializer) {
    return findOrCreate(address, EntryIndexProxy.class,
        () -> EntryIndexProxy.newInstance(address, this, serializer));
  }

  private <T extends StorageIndex> T findOrCreate(IndexAddress address, Class<T> indexType,
      Supplier<T> indexSupplier) {
    return findOpenIndex(address, indexType)
        .orElseGet(() -> createIndex(indexSupplier));
  }

  /**
   * Finds an open index by the given address and checks it has matching type.
   *
   * @param address the index address
   * @param indexType the requested index type
   * @return an index with the given address; or {@code Optional.empty()} if no index
   *     with such address was open in this access
   * @throws IllegalArgumentException if the open index has a different type from the requested
   */
  private <T extends StorageIndex> Optional<T> findOpenIndex(IndexAddress address,
      Class<T> indexType) {
    OptionalLong indexId = findIndexId(address);
    if (indexId.isPresent()) {
      // An index with the given address exists in the storage
      return indexRegistry.findIndex(indexId.getAsLong())
          .map(index -> checkedCast(index, indexType));
    } else {
      // The index does not exist; hence cannot be in the cache
      return Optional.empty();
    }
  }

  /**
   * Checks the type of the <em>cached</em> index instance and casts it to the given type.
   *
   * @param cachedIndex a cached index
   * @param requestedIndexType a index type requested for the index address
   * @throws IllegalArgumentException if the type of the cached index does not match the
   *     requested index type
   */
  private static <IndexT extends StorageIndex> IndexT checkedCast(StorageIndex cachedIndex,
      Class<IndexT> requestedIndexType) {
    checkArgument(requestedIndexType.isInstance(cachedIndex),
        "Cannot create index of type %s: the index with such address (%s) was already created"
            + "of another type (%s)", requestedIndexType, cachedIndex.getAddress(), cachedIndex);
    return requestedIndexType.cast(cachedIndex);
  }

  private <T extends StorageIndex> T createIndex(Supplier<T> indexSupplier) {
    // Create an index
    T newIndex = indexSupplier.get();
    // Find the id of the index (possibly, newly created)
    OptionalLong indexId = findIndexId(newIndex.getAddress());
    // Register the open index in the pool, if it exists.
    // It does not "exist" until it is created with a Fork-based Access,
    // i.e., an empty index created with the Snapshot will not have an id.
    // We can, of course, cache them by address then, but that is not
    // required for correctness and may be done later as an optimization.
    indexId.ifPresent(id -> registerIndex(id, newIndex));
    return newIndex;
  }

  private OptionalLong findIndexId(IndexAddress address) {
    long id = findIndexId(address.getName(), address.getIdInGroup().orElse(null));
    if (id == UNKNOWN_INDEX_ID) {
      return OptionalLong.empty();
    }
    return OptionalLong.of(id);
  }

  /**
   * Finds an internal unique MerkleDB id of an index with the given relative name
   * and optional id in group.
   * @return a non-zero id if the index is created in the database; zero if the index
   *     does not exist
   */
  private long findIndexId(String name, @Nullable byte[] idInGroup) {
    return nativeFindIndexId(getNativeHandle(), name, idInGroup);
  }

  /**
   * Registers a new index created with this access.
   */
  private void registerIndex(Long id, StorageIndex index) {
    indexRegistry.registerIndex(id, index);
  }

  /**
   * Clears the registry of open indexes.
   *
   * <p>This operation does not destroy the indexes in the registry, therefore,
   * if it might be needed to access them again, they must be destroyed separately.
   */
  void clearOpenIndexes() {
    indexRegistry.clear();
  }

  @Override
  public boolean canModify() {
    return canModify;
  }

  @Override
  public long getAccessNativeHandle() {
    return super.getNativeHandle();
  }

  /**
   * Returns the registry of open indexes for this Access.
   */
  protected OpenIndexRegistry getOpenIndexes() {
    return indexRegistry;
  }

  /**
   * Returns the cleaner of this access. It is supposed to be used with collections
   * and other objects depending on this access.
   */
  public abstract Cleaner getCleaner();

  private static native long nativeFindIndexId(long nativeHandle, String name,
      @Nullable byte[] idInGroup);

  /**
   * Returns true if the <em>native</em> Access allows modifications to the storage.
   * Note that it may differ from the Java Access {@link #canModify()} property
   * (may be more permissive, but never — stricter).
   */
  protected static native boolean nativeCanModify(long nativeHandle);
}
