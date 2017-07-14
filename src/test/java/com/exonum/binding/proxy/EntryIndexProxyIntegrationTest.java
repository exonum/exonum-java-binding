package com.exonum.binding.proxy;

import static com.exonum.binding.test.TestStorageItems.V1;
import static com.exonum.binding.test.TestStorageItems.V2;
import static com.exonum.binding.test.TestStorageItems.V3;
import static com.exonum.binding.test.TestStorageItems.bytes;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.util.LibraryLoader;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class EntryIndexProxyIntegrationTest {
  static {
    LibraryLoader.load();
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final byte[] entryPrefix = bytes("test entry");

  private Database database;

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
  public void setValue() throws Exception {
    runTestWithView(database::createFork, (e) -> {
      e.set(V1);

      assertTrue(e.isPresent());
      assertThat(e.get(), equalTo(V1));
    });
  }

  @Test
  public void setOverwritesPreviousValue() throws Exception {
    runTestWithView(database::createFork, (e) -> {
      e.set(V1);
      e.set(V2);

      assertTrue(e.isPresent());
      assertThat(e.get(), equalTo(V2));
    });
  }

  @Test
  public void setFailsWithSnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (e) -> {
      expectedException.expect(UnsupportedOperationException.class);
      e.set(V1);
    });
  }

  @Test
  public void isNotInitiallyPresent() throws Exception {
    runTestWithView(database::createSnapshot, (e) -> assertFalse(e.isPresent()));
  }

  @Test
  public void getFailsIfNotPresent() throws Exception {
    runTestWithView(database::createSnapshot, (e) -> {
      expectedException.expect(NoSuchElementException.class);
      e.get();
    });
  }

  @Test
  public void removeIfNoValue() throws Exception {
    runTestWithView(database::createFork, (e) -> {
      assertFalse(e.isPresent());
      e.remove();
      assertFalse(e.isPresent());
    });
  }

  @Test
  public void removeValue() throws Exception {
    runTestWithView(database::createFork, (e) -> {
      e.set(V3);
      e.remove();
      assertFalse(e.isPresent());
    });
  }

  @Test
  public void removeFailsWithSnapshot() throws Exception {
    runTestWithView(database::createSnapshot, (e) -> {
      expectedException.expect(UnsupportedOperationException.class);
      e.remove();
    });
  }

  @Test
  public void closeMustDetectUseAfterViewFreed() throws Exception {
    View view = database.createSnapshot();
    EntryIndexProxy entry = new EntryIndexProxy(entryPrefix, view);

    view.close();

    expectedException.expect(IllegalStateException.class);
    entry.close();
  }

  private void runTestWithView(Supplier<View> viewSupplier,
                               Consumer<EntryIndexProxy> entryTest) {
    runTestWithView(viewSupplier, (ignoredView, entry) -> entryTest.accept(entry));
  }

  private void runTestWithView(Supplier<View> viewSupplier,
                               BiConsumer<View, EntryIndexProxy> entryTest) {
    assert (database != null && database.isValid());
    try (View view = viewSupplier.get();
         EntryIndexProxy entryUnderTest = new EntryIndexProxy(entryPrefix, view)) {
      entryTest.accept(view, entryUnderTest);
    }
  }
}
