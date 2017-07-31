package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.TestStorageItems.bytes;
import static org.junit.Assert.fail;

import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.util.LibraryLoader;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Contains tests of ProofListIndexProxy methods
 * that are not present in {@link ListIndex} interface.
 */
public class ProofListIndexProxyIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Database database;

  private static final byte[] LIST_PREFIX = bytes("test proof list");

  @Before
  public void setUp() throws Exception {
    database = new MemoryDb();
  }

  @After
  public void tearDown() throws Exception {
    database.close();
  }

  @Test
  @Ignore("proofs not implemented yet")
  public void getProofTest() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      fail("not implemented");
    });
  }

  private void runTestWithView(Supplier<View> viewSupplier,
                               Consumer<ListIndex> listTest) {
    runTestWithView(viewSupplier, (ignoredView, list) -> listTest.accept(list));
  }

  private void runTestWithView(Supplier<View> viewSupplier,
                               BiConsumer<View, ListIndex> listTest) {
    IndicesTests.runTestWithView(
        viewSupplier,
        LIST_PREFIX,
        ProofListIndexProxy::new,
        listTest
    );
  }
}
