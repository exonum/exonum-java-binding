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

import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.IndexConstructors.IndexConstructorOne;
import java.util.function.BiConsumer;
import java.util.function.Function;

class IndicesTests {

  /**
   * Creates a view, an index and runs a test against the view and the index.
   * Automatically closes the view and the index. Uses String as the element type.
   *
   * @param <IndexT> type of the index
   * @param viewFactory a function creating a database view
   * @param indexName an index name
   * @param indexSupplier an index factory
   * @param indexTest a test to run. Receives the created view and the index as arguments.
   * @throws RuntimeException if the native proxies (a view or an index) failed to destroy
   *     the corresponding native objects
   */
  static <IndexT extends StorageIndex>
      void runTestWithView(Function<Cleaner, View> viewFactory,
                           String indexName,
                           IndexConstructorOne<IndexT, String> indexSupplier,
                           BiConsumer<View, IndexT> indexTest) {
    try (Cleaner cleaner = new Cleaner()) {
      // Create a view and an index.
      View view = viewFactory.apply(cleaner);
      IndexT index = indexSupplier.create(indexName, view, StandardSerializers.string());

      // Run the test
      indexTest.accept(view, index);
    } catch (CloseFailuresException e) {
      throw new RuntimeException(e);
    }
  }

  private IndicesTests() {}
}
