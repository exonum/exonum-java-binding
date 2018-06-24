package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.TestStorageItems.V1;
import static com.exonum.binding.storage.indices.TestStorageItems.V2;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.util.LibraryLoader;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class SparseListIndexProxyTest {

  static {
    LibraryLoader.load();
  }

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
  public void testEmpty() {
    runTestWithView(database::createFork, (list) -> {

      assertThat(list.size(), equalTo(0));

      assertFalse(list.get(0).isPresent());
      assertFalse(list.get(1000).isPresent());
    });
  }

  @Test
  public void addOneElement() {
    runTestWithView(database::createFork, (list) -> {
      list.add(V1);

      assertThat(list.size(), equalTo(1));
      assertThat(list.get(0).get(), equalTo(V1));
    });
  }

  @Test
  public void addTwoConsecutiveElements() {
    runTestWithView(database::createFork, (list) -> {
      list.add(V1);
      list.add(V2);

      assertThat(list.size(), equalTo(2));
      assertThat(list.get(0).get(), equalTo(V1));
      assertThat(list.get(1).get(), equalTo(V2));
      // Check not present elements
      assertFalse(list.get(2).isPresent());
    });
  }

  @Test
  public void setInEmptyList0() {
    runTestWithView(database::createFork, (list) -> {
      list.set(0, V1);

      assertThat(list.size(), equalTo(1));

      assertThat(list.get(0).get(), equalTo(V1));
    });
  }

  @Test
  public void setInEmptyList1000() {
    runTestWithView(database::createFork, (list) -> {
      list.set(1000, V1);

      assertThat(list.size(), equalTo(1));

      assertThat(list.get(1000).get(), equalTo(V1));
    });
  }

  @Test
  public void setWithGaps() {
    runTestWithView(database::createFork, (list) -> {
      list.set(1000, V1);
      list.set(2000, V2);

      assertThat(list.size(), equalTo(2));

      assertThat(list.get(1000).get(), equalTo(V1));
      assertThat(list.get(2000).get(), equalTo(V2));
      // Unknown indices:
      assertFalse(list.get(0).isPresent());
      assertFalse(list.get(1500).isPresent());
    });
  }

  @Test
  public void setAndAdd() {
    runTestWithView(database::createFork, (list) -> {
      list.set(1000, V1);
      list.add(V2);

      assertThat(list.size(), equalTo(2));

      assertThat(list.get(1000).get(), equalTo(V1));
      assertThat(list.get(1001).get(), equalTo(V2));
    });
  }

  private void runTestWithView(Supplier<View> viewFactory, Consumer<SparseListIndexProxy> test) {
    IndicesTests.runTestWithView(viewFactory,
        "list",
        SparseListIndexProxy::new,
        (view, sparseListIndexProxy) -> test.accept(sparseListIndexProxy));
  }
}
