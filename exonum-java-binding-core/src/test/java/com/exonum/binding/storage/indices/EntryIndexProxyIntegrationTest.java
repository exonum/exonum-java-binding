package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.TestStorageItems.V1;
import static com.exonum.binding.storage.indices.TestStorageItems.V2;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.serialization.StandardSerializers;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class EntryIndexProxyIntegrationTest
    extends BaseIndexProxyTestable<EntryIndexProxy<String>> {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String ENTRY_NAME = "test_entry";

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
      e.set(V1);
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

  private static void runTestWithView(Function<Cleaner, View> viewFactory,
                                      Consumer<EntryIndexProxy<String>> entryTest) {
    runTestWithView(viewFactory, (ignoredView, entry) -> entryTest.accept(entry));
  }

  private static void runTestWithView(Function<Cleaner, View> viewFactory,
                                      BiConsumer<View, EntryIndexProxy<String>> entryTest) {
    IndicesTests.runTestWithView(
        viewFactory,
        ENTRY_NAME,
        EntryIndexProxy::newInstance,
        entryTest
    );
  }

  @Override
  EntryIndexProxy<String> create(String name, View view) {
    return EntryIndexProxy.newInstance(name, view, StandardSerializers.string());
  }

  @Override
  Object getAnyElement(EntryIndexProxy<String> index) {
    return index.get();
  }
}
