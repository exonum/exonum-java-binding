package com.exonum.binding.storage.indices;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.NoSuchElementException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RustIterAdapterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  RustIterAdapter<Integer> adapter;

  @Test
  public void nextThrowsIfNoNextItem0() throws Exception {
    adapter = new RustIterAdapter<>(
        new RustIterTestFake(emptyList()));

    expectedException.expect(NoSuchElementException.class);
    adapter.next();
  }

  @Test
  public void nextThrowsIfNoNextItem1() throws Exception {
    adapter = new RustIterAdapter<>(
        new RustIterTestFake(singletonList(1)));

    adapter.next();

    expectedException.expect(NoSuchElementException.class);
    adapter.next();
  }
}
