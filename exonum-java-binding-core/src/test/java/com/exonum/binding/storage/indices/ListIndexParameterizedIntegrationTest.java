package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.TestStorageItems.V1;
import static com.exonum.binding.storage.indices.TestStorageItems.V2;
import static com.exonum.binding.test.TestParameters.parameters;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.exonum.binding.storage.database.ForkProxy;
import com.exonum.binding.storage.database.SnapshotProxy;
import com.exonum.binding.storage.database.ViewProxy;
import com.exonum.binding.storage.indices.IndexConstructors.PartiallyAppliedIndexConstructor;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
public class ListIndexParameterizedIntegrationTest
    extends BaseIndexProxyTestable<AbstractListIndexProxy<String>> {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Parameterized.Parameter(0)
  public PartiallyAppliedIndexConstructor<ListIndex<String>> listFactory;

  @Parameterized.Parameter(1)
  public String testName;

  private static final String LIST_NAME = "test_list";

  @Test
  public void addSingleElementToEmptyList() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      String addedElement = V1;
      l.add(addedElement);
      String element = l.get(0);

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
      List<String> addedElements = asList(V1, V2);
      l.addAll(addedElements);

      assertThat(Math.toIntExact(l.size()), equalTo(addedElements.size()));

      for (int i = 0; i < l.size(); i++) {
        String actual = l.get(i);
        String expected = addedElements.get(i);
        assertThat(actual, equalTo(expected));
      }
    });
  }

  @Test
  public void addAllNonEmptyCollectionNonEmptyIndex() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      int initialSize = Math.toIntExact(l.size());

      List<String> addedElements = asList(V1, V2);
      l.addAll(addedElements);

      assertThat(Math.toIntExact(l.size()), equalTo(initialSize + addedElements.size()));

      for (int i = initialSize; i < l.size(); i++) {
        String actual = l.get(i);
        String expected = addedElements.get(i - initialSize);
        assertThat(actual, equalTo(expected));
      }
    });
  }

  @Test
  public void addAllCollectionWithFirstNull() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      List<String> addedElements = asList(null, V2);
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
      List<String> addedElements = asList(V1, null);
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
      String replacingElement = "r1";
      l.set(0, replacingElement);

      String element = l.get(0);
      assertThat(element, equalTo(replacingElement));
    });
  }

  @Test
  public void setReplaceSecondLastElement() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      l.add(V2);
      String replacingElement = "r2";
      long last = l.size() - 1;
      l.set(last, replacingElement);

      String element = l.get(last);
      assertThat(element, equalTo(replacingElement));
    });
  }

  @Test
  public void setReplaceAbsentElement() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      long invalidIndex = 0;
      String replacingElement = "r2";

      expectedException.expect(IndexOutOfBoundsException.class);
      l.set(invalidIndex, replacingElement);
    });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void setWithSnapshot() throws Exception {
    // Initialize the list.
    try (ForkProxy fork = database.createFork();
         ListIndex<String> list = createList(fork)) {
      list.add(V1);
      database.merge(fork);
    }

    // Expect the read-only list to throw an exception.
    try (SnapshotProxy snapshot = database.createSnapshot();
         ListIndex<String> list = createList(snapshot)) {
      list.set(0, V2);
    }
  }

  @Test(expected = NoSuchElementException.class)
  public void getLastEmptyList() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      String ignored = l.getLast();
    });
  }

  @Test
  public void getLastSingleElementList() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      String addedElement = V1;
      l.add(addedElement);
      String last = l.getLast();

      assertThat(last, equalTo(addedElement));
    });
  }

  @Test
  public void getLastTwoElementList() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      l.add(V2);
      String last = l.getLast();

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
      List<String> elements = TestStorageItems.values;

      l.addAll(elements);

      try (StorageIterator<String> iterator = l.iterator()) {
        List<String> iterElements = ImmutableList.copyOf(iterator);

        assertThat(iterElements, equalTo(elements));
      }
    });
  }

  @Test
  @SuppressWarnings("MustBeClosedChecker")
  public void disposeShallDetectIncorrectlyClosedEvilViews() throws Exception {
    ViewProxy view = database.createSnapshot();
    ListIndex list = createList(view);

    view.close();  // a list must be closed before the corresponding view.
    expectedException.expect(IllegalStateException.class);
    list.close();
  }

  private void runTestWithView(Supplier<ViewProxy> viewSupplier,
                               Consumer<ListIndex<String>> listTest) {
    runTestWithView(viewSupplier, (ignoredView, list) -> listTest.accept(list));
  }

  private void runTestWithView(Supplier<ViewProxy> viewSupplier,
                               BiConsumer<ViewProxy, ListIndex<String>> listTest) {
    try (ViewProxy view = viewSupplier.get();
         ListIndex<String> list = createList(view)) {
      listTest.accept(view, list);
    }
  }

  @Override
  AbstractListIndexProxy<String> create(String name, ViewProxy view) {
    return (AbstractListIndexProxy<String>) listFactory.create(name, view);
  }

  private ListIndex<String> createList(ViewProxy view) {
    return listFactory.create(LIST_NAME, view);
  }

  @Parameters(name = "{index}: {1}")
  public static Collection<Object[]> testData() {
    return asList(
        parameters(IndexConstructors.from(ListIndexProxy::new), "ListIndex"),
        parameters(IndexConstructors.from(ProofListIndexProxy::new), "ProofListIndex")
    );
  }
}
