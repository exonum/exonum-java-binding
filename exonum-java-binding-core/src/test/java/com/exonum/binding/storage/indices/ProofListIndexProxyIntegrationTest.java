package com.exonum.binding.storage.indices;

import static com.exonum.binding.hash.Hashing.DEFAULT_HASH_SIZE_BITS;
import static com.exonum.binding.hash.Hashing.DEFAULT_HASH_SIZE_BYTES;
import static com.exonum.binding.storage.indices.ProofListContainsMatcher.provesThatContains;
import static com.exonum.binding.storage.indices.TestStorageItems.V1;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.util.LibraryLoader;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Contains tests of ProofListIndexProxy methods
 * that are not present in {@link ListIndex} interface.
 */
public class ProofListIndexProxyIntegrationTest {

  /**
   * An empty list root hash: an all-zero hash code.
   */
  private static final HashCode EMPTY_LIST_ROOT_HASH =
      HashCode.fromBytes(new byte[DEFAULT_HASH_SIZE_BYTES]);

  static {
    LibraryLoader.load();
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Database database;

  private static final String LIST_NAME = "test_proof_list";

  @Before
  public void setUp() throws Exception {
    database = new MemoryDb();
  }

  @After
  public void tearDown() throws Exception {
    database.close();
  }

  @Test
  public void getRootHashEmptyList() throws Exception {
    runTestWithView(database::createSnapshot, (list) -> {
      assertThat(list.getRootHash(), equalTo(EMPTY_LIST_ROOT_HASH));
    });
  }

  @Test
  public void getRootHashSingletonList() throws Exception {
    runTestWithView(database::createFork, (list) -> {
      list.add(V1);

      HashCode rootHash = list.getRootHash();
      assertThat(rootHash.bits(), equalTo(DEFAULT_HASH_SIZE_BITS));
      assertThat(rootHash, not(equalTo(EMPTY_LIST_ROOT_HASH)));
    });
  }

  @Test
  public void getProofFailsIfEmptyList() throws Exception {
    runTestWithView(database::createSnapshot, (list) -> {
      expectedException.expect(IndexOutOfBoundsException.class);
      list.getProof(0);
    });
  }

  @Test
  public void getProofSingletonList() throws Exception {
    runTestWithView(database::createFork, (list) -> {
      list.add(V1);

      assertThat(list, provesThatContains(0, V1));
    });
  }

  @Test
  public void getRangeProofSingletonList() throws Exception {
    runTestWithView(database::createFork, (list) -> {
      list.add(V1);

      assertThat(list, provesThatContains(0, singletonList(V1)));
    });
  }

  @Test
  public void getProofMultipleItemList() throws Exception {
    runTestWithView(database::createFork, (list) -> {
      List<String> values = TestStorageItems.values;

      list.addAll(values);

      for (int i = 0; i < values.size(); i++) {
        assertThat(list, provesThatContains(i, values.get(i)));
      }
    });
  }

  @Test
  public void getRangeProofMultipleItemList_FullRange() throws Exception {
    runTestWithView(database::createFork, (list) -> {
      List<String> values = TestStorageItems.values;
      list.addAll(values);

      assertThat(list, provesThatContains(0, values));
    });
  }

  @Test
  public void getRangeProofMultipleItemList_1stHalf() throws Exception {
    runTestWithView(database::createFork, (list) -> {
      List<String> values = TestStorageItems.values;
      list.addAll(values);

      int from = 0;
      int to = values.size() / 2;
      assertThat(list, provesThatContains(from, values.subList(from, to)));
    });
  }

  @Test
  public void getRangeProofMultipleItemList_2ndHalf() throws Exception {
    runTestWithView(database::createFork, (list) -> {
      List<String> values = TestStorageItems.values;
      list.addAll(values);

      int from = values.size() / 2;
      int to = values.size();
      assertThat(list, provesThatContains(from, values.subList(from, to)));
    });
  }

  @Test
  public void constructorShallNotLeakNativePeerIfSomeArgumentsAreInvalid() {
    try (View view = database.createSnapshot()) {
      // Pass null as a reference to a serializer, constructor must throw.
      try (ProofListIndexProxy<String> list =
               new ProofListIndexProxy<>(LIST_NAME, view, null)) {
        fail("Constructor must throw: " + list);
      } catch (NullPointerException | IllegalArgumentException expected) {
        // It throws indeed, but it also leaks a native peer!
        //
        // How do we assert there are no leaks?
      }
    }
  }

  private void runTestWithView(Supplier<View> viewSupplier,
                               Consumer<ProofListIndexProxy<String>> listTest) {
    runTestWithView(viewSupplier, (ignoredView, list) -> listTest.accept(list));
  }

  private void runTestWithView(Supplier<View> viewSupplier,
                               BiConsumer<View, ProofListIndexProxy<String>> listTest) {
    IndicesTests.runTestWithView(
        viewSupplier,
        LIST_NAME,
        ProofListIndexProxy::new,
        listTest
    );
  }
}
