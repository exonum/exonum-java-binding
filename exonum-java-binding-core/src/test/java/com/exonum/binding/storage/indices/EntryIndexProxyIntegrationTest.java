/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
  public void setValue() {
    runTestWithView(database::createFork, (e) -> {
      e.set(V1);

      assertTrue(e.isPresent());
      assertThat(e.get(), equalTo(V1));
    });
  }

  @Test
  public void setOverwritesPreviousValue() {
    runTestWithView(database::createFork, (e) -> {
      e.set(V1);
      e.set(V2);

      assertTrue(e.isPresent());
      assertThat(e.get(), equalTo(V2));
    });
  }

  @Test
  public void setFailsWithSnapshot() {
    runTestWithView(database::createSnapshot, (e) -> {
      expectedException.expect(UnsupportedOperationException.class);
      e.set(V1);
    });
  }

  @Test
  public void isNotInitiallyPresent() {
    runTestWithView(database::createSnapshot, (e) -> assertFalse(e.isPresent()));
  }

  @Test
  public void getFailsIfNotPresent() {
    runTestWithView(database::createSnapshot, (e) -> {
      expectedException.expect(NoSuchElementException.class);
      e.get();
    });
  }

  @Test
  public void removeIfNoValue() {
    runTestWithView(database::createFork, (e) -> {
      assertFalse(e.isPresent());
      e.remove();
      assertFalse(e.isPresent());
    });
  }

  @Test
  public void removeValue() {
    runTestWithView(database::createFork, (e) -> {
      e.set(V1);
      e.remove();
      assertFalse(e.isPresent());
    });
  }

  @Test
  public void removeFailsWithSnapshot() {
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
