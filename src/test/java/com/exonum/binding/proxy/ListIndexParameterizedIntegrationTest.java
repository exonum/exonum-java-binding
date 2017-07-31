package com.exonum.binding.proxy;

import static com.exonum.binding.test.TestParameters.parameters;
import static com.exonum.binding.test.TestStorageItems.V1;
import static com.exonum.binding.test.TestStorageItems.V2;
import static com.exonum.binding.test.TestStorageItems.bytes;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.exonum.binding.test.TestStorageItems;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * A test of common ListIndex methods.
 */
@RunWith(Parameterized.class)
public class ListIndexParameterizedIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Parameterized.Parameter(0)
  public FunctionHolder listSupplier;

  @Parameterized.Parameter(1)
  public String testName;

  private static final byte[] LIST_PREFIX = bytes("test list");

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
  public void addSingleElementToEmptyList() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      byte[] addedElement = V1;
      l.add(addedElement);
      byte[] element = l.get(0);

      assertThat(element, equalTo(addedElement));
    });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void addFailsWithSnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (l) -> l.add(V1));
  }

  @Test
  public void addAllEmptyCollection() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      l.addAll(Collections.emptyList());

      assertTrue(l.isEmpty());
    });
  }

  @Test
  public void addAllEmptyCollectionNonEmptyIndex() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      long initialSize = l.size();

      l.addAll(Collections.emptyList());

      assertThat(l.size(), equalTo(initialSize));
    });
  }

  @Test
  public void addAllNonEmptyCollection() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      List<byte[]> addedElements = asList(V1, V2);
      l.addAll(addedElements);

      assertThat(Math.toIntExact(l.size()), equalTo(addedElements.size()));

      for (int i = 0; i < l.size(); i++) {
        byte[] actual = l.get(i);
        byte[] expected = addedElements.get(i);
        assertThat(actual, equalTo(expected));
      }
    });
  }

  @Test
  public void addAllNonEmptyCollectionNonEmptyIndex() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      int initialSize = Math.toIntExact(l.size());

      List<byte[]> addedElements = asList(V1, V2);
      l.addAll(addedElements);

      assertThat(Math.toIntExact(l.size()), equalTo(initialSize + addedElements.size()));

      for (int i = initialSize; i < l.size(); i++) {
        byte[] actual = l.get(i);
        byte[] expected = addedElements.get(i - initialSize);
        assertThat(actual, equalTo(expected));
      }
    });
  }

  @Test
  public void addAllCollectionWithFirstNull() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      List<byte[]> addedElements = asList(null, V2);
      try {
        l.addAll(addedElements);
        fail("Expected NPE");
      } catch (NullPointerException e) {
        assertTrue(l.isEmpty());
      }
    });
  }

  @Test
  public void addAllCollectionWithSecondNull() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      List<byte[]> addedElements = asList(V1, null);
      try {
        l.addAll(addedElements);
        fail("Expected NPE");
      } catch (NullPointerException e) {
        assertTrue(l.isEmpty());
      }
    });
  }

  @Test
  public void addAllNullCollection() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      expectedException.expect(NullPointerException.class);
      l.addAll(null);
    });
  }

  @Test
  public void setReplaceFirstSingleElement() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      byte[] replacingElement = bytes("r1");
      l.set(0, replacingElement);

      byte[] element = l.get(0);
      assertThat(element, equalTo(replacingElement));
    });
  }

  @Test
  public void setReplaceSecondLastElement() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      l.add(V2);
      byte[] replacingElement = bytes("r2");
      long last = l.size() - 1;
      l.set(last, replacingElement);

      byte[] element = l.get(last);
      assertThat(element, equalTo(replacingElement));
    });
  }

  @Test
  public void setReplaceAbsentElement() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      long invalidIndex = 0;
      byte[] replacingElement = bytes("r2");

      expectedException.expect(IndexOutOfBoundsException.class);
      l.set(invalidIndex, replacingElement);
    });
  }

  @Test(expected = Exception.class)  // Currently it fails due to IndexOutOfBoundsException.
  // As we do not have an API to first add an item to a Fork, get a patch and apply it to the db,
  // and then test `set` with a Snapshot, we cannot test UnsupportedOperationException here.
  public void setWithSnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (l) -> l.set(0, V1));
  }

  @Test(expected = NoSuchElementException.class)
  public void getLastEmptyList() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      byte[] ignored = l.getLast();
    });
  }

  @Test
  public void getLastSingleElementList() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      byte[] addedElement = V1;
      l.add(addedElement);
      byte[] last = l.getLast();

      assertThat(last, equalTo(addedElement));
    });
  }

  @Test
  public void getLastTwoElementList() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      l.add(V2);
      byte[] last = l.getLast();

      assertThat(last, equalTo(V2));
    });
  }

  @Test
  public void clearEmptyHasNoEffect() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      l.clear();

      assertTrue(l.isEmpty());
      assertThat(l.size(), equalTo(0L));
    });
  }

  @Test
  public void clearNonEmptyRemovesAll() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      l.add(V2);
      l.clear();

      assertTrue(l.isEmpty());
      assertThat(l.size(), equalTo(0L));
    });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void clearWithSnapshot() throws Exception {
    runTestWithView(database::createSnapshot, ListIndex::clear);
  }

  @Test
  public void isEmptyWhenNew() throws Exception {
    runTestWithView(database::createSnapshot, (l) -> {
      assertTrue(l.isEmpty());
    });
  }

  @Test
  public void notEmptyAfterAdd() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      assertFalse(l.isEmpty());
    });
  }

  @Test
  public void zeroSizeWhenNew() throws Exception {
    runTestWithView(database::createSnapshot, (l) -> {
      assertThat(l.size(), equalTo(0L));
    });
  }

  @Test
  public void oneSizeAfterAdd() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      assertThat(l.size(), equalTo(1L));
    });
  }

  @Test
  public void testIterator() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      List<byte[]> elements = TestStorageItems.values;

      elements.forEach(l::add);

      try (StorageIterator<byte[]> iterator = l.iterator()) {
        List<byte[]> iterElements = ImmutableList.copyOf(iterator);

        assertThat(elements.size(), equalTo(iterElements.size()));

        for (int i = 0; i < elements.size(); i++) {
          assertThat(iterElements.get(i), equalTo(elements.get(i)));
        }
      }
    });
  }

  @Test
  public void disposeShallDetectIncorrectlyClosedEvilViews() throws Exception {
    View view = database.createSnapshot();
    ListIndex list = listSupplier.function.apply(LIST_PREFIX, view);

    view.close();  // a list must be closed before the corresponding view.
    expectedException.expect(IllegalStateException.class);
    list.close();
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
        listSupplier.function,
        listTest
    );
  }

  @Parameters(name = "{index}: {1}")
  public static Collection<Object[]> testData() {
    return asList(
        parameters(new FunctionHolder(ListIndexProxy::new), "ListIndex"),
        parameters(new FunctionHolder(ProofListIndexProxy::new), "ProofListIndex")
    );
  }

  // Needed for you can't assign a lambda to an Object
  private static class FunctionHolder {
    final BiFunction<byte[], View, ListIndex> function;

    private FunctionHolder(BiFunction<byte[], View, ListIndex> function) {
      this.function = function;
    }
  }
}
