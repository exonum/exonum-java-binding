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

import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.storage.indices.StorageIndex;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A registry of open {@linkplain com.exonum.binding.core.storage.indices indexes}. Allows
 * to de-duplicate the indexes created with the same (View, name, prefix) tuple, which is
 * required to overcome the MerkleDB limitation which prevents creating several indexes
 * with the same address (name + prefix) using the same Fork.
 */
class OpenIndexRegistry {

  private final Map<IndexAddress, StorageIndex> indexes = new HashMap<>();

  void registerIndex(StorageIndex index) {
    IndexAddress address = index.getAddress();
    Object present = indexes.putIfAbsent(address, index);
    checkArgument(present == null, "Cannot register index (%s): the address (%s) is already "
        + "associated with index (%s): ", index, address, present);
  }

  Optional<StorageIndex> findIndex(IndexAddress address) {
    return Optional.ofNullable(indexes.get(address));
  }

  void clear() {
    indexes.clear();
  }
}
