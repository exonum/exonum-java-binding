package com.exonum.binding.proxy;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

class IndicesTests {

  private IndicesTests() {}

  /**
   * Creates a view, an index and runs a test against the view and the index.
   * Automatically closes the view and the index.
   *
   * @param viewSupplier a function creating a database view
   * @param indexPrefix an index prefix
   * @param indexSupplier a function creating an index from the prefix and the database view
   * @param indexTest a test to run. Receives the created view and the index as arguments.
   * @param <I> type of the index
   */
  static <I extends NativeProxy>
      void runTestWithView(Supplier<View> viewSupplier,
                           byte[] indexPrefix,
                           BiFunction<byte[], View, I> indexSupplier,
                           BiConsumer<View, I> indexTest) {
    try (View view = viewSupplier.get();
         I index = indexSupplier.apply(indexPrefix, view)) {
      indexTest.accept(view, index);
    }
  }
}
