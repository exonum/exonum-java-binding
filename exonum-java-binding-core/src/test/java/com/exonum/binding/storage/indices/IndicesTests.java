package com.exonum.binding.storage.indices;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.proxy.NativeProxy;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.IndexConstructors.IndexConstructorOne;
import com.exonum.binding.storage.serialization.StandardSerializers;
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
  static <IndexT extends NativeProxy>
      void runTestWithView(Function<Cleaner, View> viewFactory,
                           String indexName,
                           IndexConstructorOne<IndexT, String> indexSupplier,
                           BiConsumer<View, IndexT> indexTest) {
    try (Cleaner cleaner = new Cleaner()) {
      View view = viewFactory.apply(cleaner);

      try (IndexT index = indexSupplier.create(indexName, view, StandardSerializers.string())) {
        indexTest.accept(view, index);
      }

    } catch (CloseFailuresException e) {
      throw new RuntimeException(e);
    }
  }

  private IndicesTests() {}
}
