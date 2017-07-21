package com.exonum.binding.proxy;

import static com.exonum.binding.test.TestStorageItems.V1;
import static com.exonum.binding.test.TestStorageItems.V2;
import static com.exonum.binding.test.TestStorageItems.V9;
import static com.exonum.binding.test.TestStorageItems.bytes;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.storage.RustIterAdapter;
import com.exonum.binding.storage.StorageIterator;
import com.exonum.binding.test.TestStorageItems;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedBytes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ValueSetIndexProxyIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String EXONUM_DEFAULT_HASHING_ALGORITHM = "SHA-256";

  private static final byte[] VALUE_SET_PREFIX = bytes("test value set");

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
      set.add(V1);
      assertTrue(set.contains(V1));
    });
  }

  @Test
  public void addFailsIfSnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (set) -> {
      expectedException.expect(UnsupportedOperationException.class);
      set.add(V1);
    });
  }

  @Test
  public void clearEmptyHasNoEffect() throws Exception {
    runTestWithView(database::createFork, ValueSetIndexProxy::clear);
  }

  @Test
  public void clearNonEmptyRemovesAllElements() throws Exception {
    runTestWithView(database::createFork, (set) -> {
      List<byte[]> elements = TestStorageItems.values.subList(0, 3);

      elements.forEach(set::add);

      set.clear();

      elements.forEach(
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
    runTestWithView(database::createSnapshot, (set) -> assertFalse(set.contains(V2)));
  }

  @Test
  public void doesNotContainAbsentElement() throws Exception {
    runTestWithView(database::createFork, (set) -> {
      set.add(V1);

      assertFalse(set.contains(V2));
    });
  }

  @Test
  public void doesNotContainElementsByHashWhenEmpty() throws Exception {
    runTestWithView(database::createSnapshot, (set) -> {
      byte[] valueHash = getHashOf(V2);
      assertFalse(set.containsByHash(valueHash));
    });
  }

  @Test
  public void containsByHash() throws Exception {
    runTestWithView(database::createFork, (set) -> {
      set.add(V1);

      byte[] valueHash = getHashOf(V1);
      assertTrue(set.containsByHash(valueHash));
    });
  }

  @Test
  public void doesNotContainAbsentElementsByHash() throws Exception {
    runTestWithView(database::createFork, (set) -> {
      set.add(V1);

      byte[] otherValueHash = getHashOf(V2);
      assertFalse(set.containsByHash(otherValueHash));
    });
  }

  @Test
  public void testHashesIter() throws Exception {
    runTestWithView(database::createFork, (set) -> {
      List<byte[]> elements = TestStorageItems.values;

      elements.forEach(set::add);

      try (StorageIterator<byte[]> iter = new RustIterAdapter<>(set.hashes())) {
        List<byte[]> iterHashes = ImmutableList.copyOf(iter);

        // Check that there are as many hashes as elements
        assertThat(elements.size(), equalTo(iterHashes.size()));

        // Check that all hashes correspond to elements in the set.
        for (byte[] hash: iterHashes) {
          assertTrue(set.containsByHash(hash));
        }

        // Check that hashes appear in lexicographical order
        List<byte[]> orderedHashes = getOrderedHashes(elements);
        for (int i = 0; i < elements.size(); i++) {
          assertThat(iterHashes.get(i), equalTo(orderedHashes.get(i)));
        }
      }
    });
  }

  @Test
  public void testIterator() throws Exception {
    runTestWithView(database::createFork, (set) -> {
      List<byte[]> elements = TestStorageItems.values;

      elements.forEach(set::add);

      try (StorageIterator<ValueSetIndexProxy.Entry> iterator =
                   new RustIterAdapter<>(set.iterator())) {
        List<ValueSetIndexProxy.Entry> entriesFromIter = ImmutableList.copyOf(iterator);

        assertThat(entriesFromIter.size(), equalTo(elements.size()));

        for (ValueSetIndexProxy.Entry e : entriesFromIter) {
          assertTrue(set.containsByHash(e.getHash()));
          assertTrue(set.contains(e.getValue()));
        }

        for (ValueSetIndexProxy.Entry e : entriesFromIter) {
          assertThat(e.getHash(), equalTo(getHashOf(e.getValue())));
        }

        List<byte[]> orderedHashes = getOrderedHashes(elements);
        for (int i = 0; i < elements.size(); i++) {
          byte[] hashFromIter = entriesFromIter.get(i).getHash();
          byte[] expectedHash = orderedHashes.get(i);
          assertThat(hashFromIter, equalTo(expectedHash));
        }
      }
    });
  }

  private static List<byte[]> getOrderedHashes(List<byte[]> elements) {
    return elements.stream()
            .map(ValueSetIndexProxyIntegrationTest::getHashOf)
            .sorted(UnsignedBytes.lexicographicalComparator())
            .collect(Collectors.toList());
  }

  @Test
  public void removesAddedElement() throws Exception {
    runTestWithView(database::createFork, (set) -> {
      set.add(V1);

      set.remove(V1);

      assertFalse(set.contains(V1));
    });
  }

  @Test
  public void removeAbsentElementDoesNothing() throws Exception {
    runTestWithView(database::createFork, (set) -> {
      set.add(V1);

      set.remove(V9);

      assertFalse(set.contains(V9));
      assertTrue(set.contains(V1));
    });
  }

  @Test
  public void removeFailsIfSnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (set) -> {
      expectedException.expect(UnsupportedOperationException.class);
      set.remove(V1);
    });
  }

  @Test
  public void removesAddedElementByHash() throws Exception {
    runTestWithView(database::createFork, (set) -> {
      set.add(V1);

      byte[] valueHash = getHashOf(V1);

      set.removeByHash(valueHash);

      assertFalse(set.contains(V1));
      assertFalse(set.containsByHash(valueHash));
    });
  }

  @Test
  public void removeAbsentElementByHashDoesNothing() throws Exception {
    runTestWithView(database::createFork, (set) -> {
      set.add(V1);

      byte[] valueHash = getHashOf(V9);
      set.remove(valueHash);

      assertFalse(set.contains(V9));
      assertFalse(set.containsByHash(valueHash));
      assertTrue(set.contains(V1));
    });
  }

  @Test
  public void removeByHashFailsIfSnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (set) -> {
      expectedException.expect(UnsupportedOperationException.class);
      set.removeByHash(getHashOf(V1));
    });
  }

  @Test
  public void disposeShallDetectIncorrectlyClosedEvilViews() throws Exception {
    View view = database.createSnapshot();
    ValueSetIndexProxy set = new ValueSetIndexProxy(VALUE_SET_PREFIX, view);

    view.close();  // a set must be closed before the corresponding view.
    expectedException.expect(IllegalStateException.class);
    set.close();
  }


  /**
   * Creates a view, a value set index and runs a test against the view and the set.
   * Automatically closes the view and the set.
   *
   * @param viewSupplier a function creating a database view
   * @param valueSetTest a test to run. Receives the created set as an argument.
   */
  private void runTestWithView(Supplier<View> viewSupplier,
                               Consumer<ValueSetIndexProxy> valueSetTest) {
    runTestWithView(viewSupplier,
        (view, valueSetUnderTest) -> valueSetTest.accept(valueSetUnderTest)
    );
  }

  /**
   * Creates a view, a value set index and runs a test against the view and the set.
   * Automatically closes the view and the set.
   *
   * @param viewSupplier a function creating a database view
   * @param valueSetTest a test to run. Receives the created view and the set as arguments.
   */
  private void runTestWithView(Supplier<View> viewSupplier,
                               BiConsumer<View, ValueSetIndexProxy> valueSetTest) {
    IndicesTests.runTestWithView(
        viewSupplier,
        VALUE_SET_PREFIX,
        ValueSetIndexProxy::new,
        valueSetTest
    );
  }

  private static byte[] getHashOf(byte[] value) {
    try {
      MessageDigest sha256 = MessageDigest.getInstance(EXONUM_DEFAULT_HASHING_ALGORITHM);
      return sha256.digest(value);
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }
}
