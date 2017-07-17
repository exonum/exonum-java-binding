package com.exonum.binding.proxy;

import static com.exonum.binding.test.TestStorageItems.V1;
import static com.exonum.binding.test.TestStorageItems.V2;
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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ListIndexProxyIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Rule public ExpectedException expectedException = ExpectedException.none();

  private Database database;

  private final byte[] listPrefix = bytes("test list");

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
  public void addSingleElement() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      byte[] addedElement = V1;
      l.add(addedElement);
      byte[] element = l.get(0);

      assertThat(element, equalTo(addedElement));
    });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void addWithSnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (l) -> l.add(V1));
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

      byte[] element = l.getLast();
      assertThat(element, equalTo(replacingElement));
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

  @Test(expected = NoSuchElementException.class)
  public void removeLastEmptyList() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      byte[] ignored = l.removeLast();
    });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void removeLastWithSnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (l) -> {
      byte[] ignored = l.removeLast();
    });
  }

  @Test
  public void removeLastSingleElementList() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      byte[] addedElement = V1;
      l.add(addedElement);
      byte[] last = l.removeLast();

      assertThat(last, equalTo(addedElement));
      assertTrue(l.isEmpty());
    });
  }

  @Test
  public void removeLastTwoElementList() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      l.add(V2);
      byte[] last = l.removeLast();

      assertThat(last, equalTo(V2));
      assertThat(l.size(), equalTo(1L));
      assertThat(l.get(0), equalTo(V1));
    });
  }

  @Test
  public void truncateNonEmptyToZero() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      l.truncate(0);

      assertTrue(l.isEmpty());
      assertThat(l.size(), equalTo(0L));
    });
  }

  @Test
  public void truncateToSameSize() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      long newSize = 1;
      l.add(V1);
      l.truncate(newSize);

      assertThat(l.size(), equalTo(newSize));
    });
  }

  @Test
  public void truncateToSmallerSize() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      long newSize = 1;
      l.add(V1);
      l.add(V2);
      l.truncate(newSize);

      assertThat(l.size(), equalTo(newSize));
    });
  }

  @Test
  public void truncateToGreaterSizeHasNoEffect() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      l.add(V1);
      l.add(V2);
      long oldSize = l.size();
      long newSize = 4;
      l.truncate(newSize);

      assertThat(l.size(), equalTo(oldSize));
    });
  }

  @Test(expected = IllegalArgumentException.class)
  public void truncateToNegativeThrows() throws Exception {
    runTestWithView(database::createFork, (l) -> {
      long invalidSize = -1;
      l.add(V1);
      l.truncate(invalidSize);
    });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void truncateWithSnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (l) -> l.truncate(0L));
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
    runTestWithView(database::createSnapshot, ListIndexProxy::clear);
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

      try (RustIter<byte[]> rustIter = l.iterator();
           StorageIterator<byte[]> iter = new RustIterAdapter<>(rustIter)) {
        List<byte[]> iterElements = ImmutableList.copyOf(iter);

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
    ListIndexProxy list = new ListIndexProxy(listPrefix, view);

    view.close();  // a list must be closed before the corresponding view.
    expectedException.expect(IllegalStateException.class);
    list.close();
  }

  private void runTestWithView(Supplier<View> viewSupplier,
                               Consumer<ListIndexProxy> listTest) {
    runTestWithView(viewSupplier, (ignoredView, list) -> listTest.accept(list));
  }

  private void runTestWithView(Supplier<View> viewSupplier,
                               BiConsumer<View, ListIndexProxy> listTest) {
    try (View view = viewSupplier.get();
         ListIndexProxy listUnderTest = new ListIndexProxy(listPrefix, view)) {
      listTest.accept(view, listUnderTest);
    }
  }
}
