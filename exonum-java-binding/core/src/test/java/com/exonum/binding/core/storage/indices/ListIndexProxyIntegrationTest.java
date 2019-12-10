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

import static com.exonum.binding.core.storage.indices.TestStorageItems.V1;

import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.storage.database.View;

/**
 * Inherits base tests of ListIndex interface methods and may contain
 * tests for methods that are not present in {@link ListIndex} interface.
 */
class ListIndexProxyIntegrationTest extends BaseListIndexIntegrationTestable {

  @Override
  ListIndexProxy<String> create(String name, View view) {
    return ListIndexProxy.newInstance(name, view, StandardSerializers.string());
  }

  @Override
  ListIndexProxy<String> createInGroup(String groupName, byte[] idInGroup, View view) {
    return ListIndexProxy.newInGroupUnsafe(groupName, idInGroup, view,
        StandardSerializers.string());
  }

  @Override
  StorageIndex createOfOtherType(String name, View view) {
    return EntryIndexProxy.newInstance(name, view, StandardSerializers.string());
  }

  @Override
  Object getAnyElement(AbstractListIndexProxy<String> index) {
    return index.get(0L);
  }

  @Override
  void update(AbstractListIndexProxy<String> index) {
    index.add(V1);
  }
}
