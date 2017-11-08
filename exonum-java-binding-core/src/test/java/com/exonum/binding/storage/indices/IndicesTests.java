package com.exonum.binding.storage.indices;

import com.exonum.binding.proxy.NativeProxy;
import com.exonum.binding.storage.database.View;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

class IndicesTests {

  private IndicesTests() {}

  /**
   * Creates a view, an index and runs a test against the view and the index.
   * Automatically closes the view and the index.
   *
   * @param <I> type of the index
   * @param viewSupplier a function creating a database view
   * @param indexName an index name
   * @param indexSupplier a function creating an index from the name and the database view
   * @param indexTest a test to run. Receives the created view and the index as arguments.
   */
  static <I extends NativeProxy>
      void runTestWithView(Supplier<View> viewSupplier,
                           String indexName,
                           BiFunction<String, View, I> indexSupplier,
                           BiConsumer<View, I> indexTest) {
    try (View view = viewSupplier.get();
         I index = indexSupplier.apply(indexName, view)) {
      indexTest.accept(view, index);
    }
  }
}
