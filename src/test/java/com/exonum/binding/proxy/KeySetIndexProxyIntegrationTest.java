package com.exonum.binding.proxy;

import static com.exonum.binding.test.TestStorageItems.K1;
import static com.exonum.binding.test.TestStorageItems.K9;
import static com.exonum.binding.test.TestStorageItems.bytes;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.test.TestStorageItems;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class KeySetIndexProxyIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final byte[] KEY_SET_PREFIX = bytes("test key set");

  private Database database;

  @Before
  public void setUp() throws Exception {
    database = new MemoryDb();
  }

  @After
  public void tearDown() throws Exception {
    database.close();
  }

  @Test
  public void addSingleElement() throws Exception {
    runTestWithView(database::createFork, (set) -> {
      set.add(K1);
      assertTrue(set.contains(K1));
    });
  }

  @Test
  public void addMultipleElements() throws Exception {
    runTestWithView(database::createFork, (set) -> {
      List<byte[]> keys = TestStorageItems.keys.subList(0, 3);
      keys.forEach(set::add);
      keys.forEach(
          (k) -> assertTrue(set.contains(k))
      );
    });
  }

  @Test
  public void addFailsIfSnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (set) -> {
      expectedException.expect(UnsupportedOperationException.class);
      set.add(K1);
    });
  }

  @Test
  public void clearEmptyHasNoEffect() throws Exception {
    runTestWithView(database::createFork, KeySetIndexProxy::clear);
  }

  @Test
  public void clearNonEmptyRemovesAllElements() throws Exception {
    runTestWithView(database::createFork, (set) -> {
      List<byte[]> keys = TestStorageItems.keys.subList(0, 3);

      keys.forEach(set::add);

      set.clear();

      keys.forEach(
          (k) -> assertFalse(set.contains(k))
      );
    });
  }

  @Test
  public void clearFailsIfSnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (set) -> {
      expectedException.expect(UnsupportedOperationException.class);
      set.clear();
    });
  }

  @Test
  public void doesNotContainElementsWhenEmpty() throws Exception {
    runTestWithView(database::createSnapshot, (set) -> assertFalse(set.contains(K1)));
  }

  @Test
  public void testIterator() throws Exception {
    runTestWithView(database::createFork, (set) -> {
      List<byte[]> elements = TestStorageItems.keys;

      elements.forEach(set::add);

      try (StorageIterator<byte[]> iterator = set.iterator()) {
        List<byte[]> iterElements = ImmutableList.copyOf(iterator);

        // Check that there are as many elements as expected
        assertThat(elements.size(), equalTo(iterElements.size()));

        // Check that all elements are in the set.
        for (byte[] e: iterElements) {
          assertTrue(set.contains(e));
        }

        // Check that elements appear in lexicographical order
        for (int i = 0; i < elements.size(); i++) {
          assertThat(iterElements.get(i), equalTo(elements.get(i)));
        }
      }
    });
  }

  @Test
  public void removesAddedElement() throws Exception {
    runTestWithView(database::createFork, (set) -> {
      set.add(K1);

      set.remove(K1);

      assertFalse(set.contains(K1));
    });
  }

  @Test
  public void removeNotPresentElementDoesNothing() throws Exception {
    runTestWithView(database::createFork, (set) -> {
      set.add(K1);

      set.remove(K9);

      assertFalse(set.contains(K9));
      assertTrue(set.contains(K1));
    });
  }

  @Test
  public void removeFailsIfSnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (set) -> {
      expectedException.expect(UnsupportedOperationException.class);
      set.remove(K1);
    });
  }

  @Test
  public void disposeShallDetectIncorrectlyClosedEvilViews() throws Exception {
    View view = database.createSnapshot();
    KeySetIndexProxy set = new KeySetIndexProxy(KEY_SET_PREFIX, view);

    view.close();  // a set must be closed before the corresponding view.
    expectedException.expect(IllegalStateException.class);
    set.close();
  }

  /**
   * Creates a view, a key set index and runs a test against the view and the set.
   * Automatically closes the view and the set.
   *
   * @param viewSupplier a function creating a database view
   * @param keySetTest a test to run. Receives the created set as an argument.
   */
  private static void runTestWithView(Supplier<View> viewSupplier,
                                      Consumer<KeySetIndexProxy> keySetTest) {
    runTestWithView(viewSupplier, (view, keySetUnderTest) -> keySetTest.accept(keySetUnderTest));
  }

  /**
   * Creates a view, a key set index and runs a test against the view and the set.
   * Automatically closes the view and the set.
   *
   * @param viewSupplier a function creating a database view
   * @param keySetTest a test to run. Receives the created view and the set as arguments.
   */
  private static void runTestWithView(Supplier<View> viewSupplier,
                                      BiConsumer<View, KeySetIndexProxy> keySetTest) {
    IndicesTests.runTestWithView(
        viewSupplier,
        KEY_SET_PREFIX,
        KeySetIndexProxy::new,
        keySetTest
    );
  }
}
