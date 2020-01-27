/*
 * Copyright 2020 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.core.storage.database;

import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.storage.indices.KeySetIndexProxy;
import com.exonum.binding.core.storage.indices.ListIndexProxy;
import com.exonum.binding.core.storage.indices.MapIndexProxy;
import com.exonum.binding.core.storage.indices.ProofEntryIndexProxy;
import com.exonum.binding.core.storage.indices.ProofListIndexProxy;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.core.storage.indices.ValueSetIndexProxy;

/**
 * Provides <em>access</em> to Exonum MerkleDB indexes. An access object corresponds to
 * a certain database state.
 *
 * <p>An access can be read-only or read-write. Read-only accesses produce indexes that
 * <a href="../indices/package-summary.html#modifications">
 * forbid modifying operations.
 * </a>
 *
 * <p>The changes made to read-write accesses are not usually applied immediately to the database
 * state, but are performed separately. For example, Exonum will apply the changes
 * made by all transactions when a block is confirmed.
 *
 * <p>Accesses may perform <em>index address resolution</em>: they may modify the passed index
 * address before fetching it from the database. That implies that addresses passed to index factory
 * methods are <em>relative</em> to an access object. The address resolution rules
 * must be documented in interface implementations.
 *
 * <p>As each Access object requires some MerkleDB resources to function, they work
 * in a {@linkplain Cleaner scope} that is usually managed by the framework.
 * When an Access is closed, all indexes created with it are destroyed and become inaccessible;
 * and no new indexes can be created.
 *
 * <p>All method arguments are non-null by default.
 *
 * <hr/>
 *
 * <p>This Java interface is similar to a combination of Rust {@code Access} and {@code AccessExt}
 * traits.
 *
 * @see com.exonum.binding.core.storage.indices
 * @see StandardSerializers
 */
public interface Access {

  /**
   * Creates a new ProofListIndex.
   *
   * @param address an index address in the MerkleDB
   * @param serializer a serializer of list elements
   * @param <E> the type of elements in this list
   * @throws IllegalStateException if this access is not valid
   * @see StandardSerializers
   */
  <E> ProofListIndexProxy<E> getProofList(IndexAddress address, Serializer<E> serializer);

  /**
   * Creates a new ListIndex.
   *
   * @param address an index address in the MerkleDB
   * @param serializer a serializer of list elements
   * @param <E> the type of elements in this list
   * @throws IllegalStateException if this access is not valid
   * @see #getProofList(IndexAddress, Serializer)
   * @see StandardSerializers
   */
  <E> ListIndexProxy<E> getList(IndexAddress address, Serializer<E> serializer);

  /**
   * Creates a new ProofMapIndex.
   *
   * @param address an index address in the MerkleDB
   * @param keySerializer a serializer of keys
   * @param valueSerializer a serializer of values
   * @param <K> the type of keys in the map
   * @param <V> the type of values in the map
   * @throws IllegalStateException if this access is not valid
   * @see StandardSerializers
   */
  <K, V> ProofMapIndexProxy<K, V> getProofMap(IndexAddress address, Serializer<K> keySerializer,
      Serializer<V> valueSerializer);

  /**
   * Creates a new <a href="../indices/ProofMapIndexProxy.html#key-hashing">"raw" ProofMapIndex</a>.
   * A raw ProofMapIndex does not hash keys, hence imposes some requirements on them.
   *
   * @param address an index address in the MerkleDB
   * @param keySerializer a serializer of keys, must always produce 32-byte long values
   *     that suit the requirements
   * @param valueSerializer a serializer of values
   * @param <K> the type of keys in the map
   * @param <V> the type of values in the map
   * @throws IllegalStateException if this access is not valid
   * @see #getProofMap(IndexAddress, Serializer, Serializer)
   * @see StandardSerializers
   */
  <K, V> ProofMapIndexProxy<K, V> getRawProofMap(IndexAddress address, Serializer<K> keySerializer,
      Serializer<V> valueSerializer);

  /**
   * Creates a new MapIndex.
   *
   * @param address an index address in the MerkleDB
   * @param keySerializer a serializer of keys
   * @param valueSerializer a serializer of values
   * @param <K> the type of keys in the map
   * @param <V> the type of values in the map
   * @throws IllegalStateException if this access is not valid
   * @see #getProofMap(IndexAddress, Serializer, Serializer)
   * @see StandardSerializers
   */
  <K, V> MapIndexProxy<K, V> getMap(IndexAddress address, Serializer<K> keySerializer,
      Serializer<V> valueSerializer);

  /**
   * Creates a new KeySet.
   *
   * @param address an index address in the MerkleDB
   * @param serializer a serializer of set keys
   * @param <E> the type of keys in this set
   * @throws IllegalStateException if this access is not valid
   * @see #getValueSet(IndexAddress, Serializer)
   * @see StandardSerializers
   */
  <E> KeySetIndexProxy<E> getKeySet(IndexAddress address, Serializer<E> serializer);

  /**
   * Creates a new ValueSet.
   *
   * @param address an index address in the MerkleDB
   * @param serializer a serializer of set values
   * @param <E> the type of values in this set
   * @throws IllegalStateException if this access is not valid
   * @see #getKeySet(IndexAddress, Serializer)
   * @see StandardSerializers
   */
  <E> ValueSetIndexProxy<E> getValueSet(IndexAddress address, Serializer<E> serializer);

  /**
   * Creates a new ProofEntry.
   *
   * @param address an index address in the MerkleDB
   * @param serializer an entry serializer
   * @param <E> the type of the entry
   * @throws IllegalStateException if this access is not valid
   * @see StandardSerializers
   */
  <E> ProofEntryIndexProxy<E> getProofEntry(IndexAddress address, Serializer<E> serializer);

  /**
   * Returns true if this access allows modifications to the database state; false if it is
   * immutable.
   */
  boolean canModify();

  /**
   *  Returns a native handle of this access.
   *
   *  @throws IllegalStateException if the access is invalid (closed or nullptr)
   */
  long getAccessNativeHandle();
}
