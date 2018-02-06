package com.exonum.binding.storage.indices;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.View;
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

  // fixme: move them back to Storage values in non-WIP PR
  private static final String V1 = "v1";
  private static final String V2 = "v2";
  private static final String V3 = "v3";

  static {
    LibraryLoader.load();
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String ENTRY_NAME = "test_entry";

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
    EntryIndexProxy<String> entry = new EntryIndexProxy<>(ENTRY_NAME, view,
        TestSerializers.string());

    view.close();

    expectedException.expect(IllegalStateException.class);
    entry.close();
  }

  private static void runTestWithView(Supplier<View> viewSupplier,
                                      Consumer<EntryIndexProxy<String>> entryTest) {
    runTestWithView(viewSupplier, (ignoredView, entry) -> entryTest.accept(entry));
  }

  private static void runTestWithView(Supplier<View> viewSupplier,
                                      BiConsumer<View, EntryIndexProxy<String>> entryTest) {
    // todo: it would be nice to migrate run test to a new signature, again, NON-WIP PR
    try (View view = viewSupplier.get();
         EntryIndexProxy<String> entry =
             new EntryIndexProxy<>(ENTRY_NAME, view, TestSerializers.string())) {
      entryTest.accept(view, entry);
    }
  }
}
