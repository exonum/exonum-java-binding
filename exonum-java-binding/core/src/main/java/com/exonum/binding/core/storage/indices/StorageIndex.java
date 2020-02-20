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

/**
 * Storage index is a persistent, named collection built on top of Exonum key-value storage.
 *
 * <p>Also known as a collection, a table, and also as (rarely) a database view.
 */
public interface StorageIndex {

  /**
   * Returns the name of this index.
   *
   * <p>Please note that the implementations may return either relative or absolute name.
   * The name is not required to be equal to the one passed to the index constructor.
   * <!-- This vague-ish clause and strange behaviour are needed to allow index caching across
   *      Accesses -->
   */
  default String getName() {
    return getAddress().getName();
  }

  /**
   * Returns the <em>index address</em>: its identifier in the database.
   *
   * <p>Please note that the implementations may return either relative or absolute address.
   * The address is not required to be equal to the one passed to the index constructor.
   * <!-- This vague-ish clause and strange behaviour are needed to allow index caching across
   *      Accesses. -->
   */
  IndexAddress getAddress();
}
