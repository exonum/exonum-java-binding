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

import com.exonum.binding.core.storage.database.AbstractAccess;

/**
 * Storage index is a persistent, named collection built on top of Exonum key-value storage.
 *
 * <p>Also known as a collection, a table, and also as (rarely) a view for
 * <!-- todo: rewrite the 'why' -->
 * a {@linkplain AbstractAccess database view} is inherently associated with an index.
 */
public interface StorageIndex {

  /** Returns the name of this index. */
  default String getName() {
    return getAddress().getName();
  }

  /**
   * Returns the <em>index address</em>: its unique identifier in the database. It consists
   * of the name and, in case this index belongs to an index family, a family identifier.
   */
  IndexAddress getAddress();
}
