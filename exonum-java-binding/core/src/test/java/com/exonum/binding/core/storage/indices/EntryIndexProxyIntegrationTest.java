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

package com.exonum.binding.core.storage.indices;

import static com.exonum.binding.common.serialization.StandardSerializers.string;

import com.exonum.binding.core.storage.database.Access;

public class EntryIndexProxyIntegrationTest extends BaseEntryIndexProxyIntegrationTest<EntryIndex<String>> {

  @Override
  EntryIndex<String> create(String name, Access access) {
    return access.getEntry(IndexAddress.valueOf(name), string());
  }
}
