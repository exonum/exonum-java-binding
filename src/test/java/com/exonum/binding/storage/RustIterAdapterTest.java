package com.exonum.binding.storage;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.spy;

import com.exonum.binding.proxy.RustIter;
import com.exonum.binding.proxy.RustIterTestFake;
import java.util.NoSuchElementException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest(RustIterAdapter.class)
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

  @Test
  public void closesTheUnderlyingIter() throws Exception {
    RustIter<Integer> rustIter = spy(new RustIterTestFake(emptyList()));

    adapter = new RustIterAdapter<>(rustIter);
    adapter.close();

    verify(rustIter).close();
  }
}
