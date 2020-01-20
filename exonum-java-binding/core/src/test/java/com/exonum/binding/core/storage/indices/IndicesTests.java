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

import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.storage.database.AbstractAccess;
import java.util.function.BiConsumer;
import java.util.function.Function;

class IndicesTests {

  /**
   * Creates an access, an index and runs a test against the access and the index.
   * Automatically closes the access and the index. Uses String as the element type.
   *
   * @param <IndexT> type of the index
   * @param accessFactory a function creating a database access
   * @param indexName an index name
   * @param indexSupplier an index factory
   * @param indexTest a test to run. Receives the created access and the index as arguments.
   * @throws RuntimeException if the native proxies (an access or an index) failed to destroy
   *     the corresponding native objects
   */
  static <IndexT extends StorageIndex>
      void runTestWithView(Function<Cleaner, AbstractAccess> accessFactory,
                           String indexName,
                           IndexConstructorOne<IndexT, String> indexSupplier,
                           BiConsumer<AbstractAccess, IndexT> indexTest) {
    try (Cleaner cleaner = new Cleaner()) {
      // Create an access and an index.
      AbstractAccess access = accessFactory.apply(cleaner);
      IndexT index = indexSupplier.create(indexName, access, StandardSerializers.string());

      // Run the test
      indexTest.accept(access, index);
    } catch (CloseFailuresException e) {
      throw new RuntimeException(e);
    }
  }

  private IndicesTests() {}
}
