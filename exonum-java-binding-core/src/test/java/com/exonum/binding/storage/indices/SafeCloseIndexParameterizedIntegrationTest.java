package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.ProofMapIndexProxyIntegrationTest.PK1;
import static com.exonum.binding.storage.indices.TestStorageItems.K1;
import static com.exonum.binding.storage.indices.TestStorageItems.V1;
import static java.util.Arrays.asList;

import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.util.LibraryLoader;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

// The order in which objects are created:
//
// Database > View > Index > (opt) Iterator
//
// MemoryDb
//            Snapshot & Fork
//
//                   BiFunction<String, View, Index>
@RunWith(Parameterized.class)
public class SafeCloseIndexParameterizedIntegrationTest<I extends AbstractIndexProxy> {

  static {
    LibraryLoader.load();
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String indexName = "index";

  private MemoryDb database;

  private Function<View, I> indexProvider;

  private Consumer<I> indexConsumer;

  public SafeCloseIndexParameterizedIntegrationTest(TestParameters<I> parameters) {
    this.indexProvider = (v) -> parameters.indexCtor.apply(indexName, v);
    this.indexConsumer = parameters.indexConsumer;
  }

  @Before
  public void setUp() throws Exception {
    database = new MemoryDb();
  }

  @After
  public void tearDown() throws Exception {
    if (database != null) {
      database.close();
    }
  }

  @Test
  public void happyPath() throws Exception {
    try (Snapshot snapshot = database.createSnapshot();
         I index = indexProvider.apply(snapshot)) {
      indexConsumer.accept(index);
    }
  }

  @Test
  public void closeSnapshotBeforeCollection() throws Exception {
    try (Snapshot view = database.createSnapshot();
         I index = indexProvider.apply(view)) {

      // Must do that after index
      // todo: Maybe throw earlier (= here), when you try to close an object *other* objects
      // depend on (v2)?
      view.close();

      expectedException.expect(IllegalStateException.class);
      indexConsumer.accept(index);
    }
  }

  @Test
  public void closeForkBeforeCollection() throws Exception {
    try (Fork view = database.createFork();
         I index = indexProvider.apply(view)) {

      // Must do that after index
      view.close();

      expectedException.expect(IllegalStateException.class);
      indexConsumer.accept(index);
    }
  }

  @Test
  public void closeEverything() throws Exception {
    Fork view = database.createFork();
    I index = indexProvider.apply(view);
    index.close();
    view.close();

    expectedException.expect(IllegalStateException.class);
    indexConsumer.accept(index);
  }

  @Test
  public void closeMemoryDbBeforeAnything() throws Exception {
    Snapshot view = database.createSnapshot();
    I index = indexProvider.apply(view);

    database.close();

    expectedException.expect(IllegalStateException.class);
    indexConsumer.accept(index);
  }

  @Parameters
  public static Collection<Object[]> data() {
    return asList(
        parameters(ListIndexProxy::new, AbstractListIndexProxy::size),

        parameters(ProofListIndexProxy::new, AbstractListIndexProxy::size),

        parameters(MapIndexProxy::new, (map) -> map.containsKey(K1)),

        parameters(ProofMapIndexProxy::new, (map) -> map.containsKey(PK1)),

        parameters(KeySetIndexProxy::new, (set) -> set.contains(K1)),

        parameters(ValueSetIndexProxy::new, (set) -> set.contains(V1)),

        parameters((name, view) -> new EntryIndexProxy<>(name, view, TestSerializers.string()),
            (entry) -> entry.isPresent())
    );
  }

  private static <I extends AbstractIndexProxy> TestParameters[] parameters(
      BiFunction<String, View, I> indexCtor,
      Consumer<I> indexConsumer) {
    return new TestParameters[] {
        new TestParameters<>(indexCtor, indexConsumer)
    };
  }

  static class TestParameters<J extends AbstractIndexProxy> {
    BiFunction<String, View, J> indexCtor;

    Consumer<J> indexConsumer;

    TestParameters(BiFunction<String, View, J> indexCtor,
                   Consumer<J> indexConsumer) {
      this.indexCtor = indexCtor;
      this.indexConsumer = indexConsumer;
    }
  }
}
