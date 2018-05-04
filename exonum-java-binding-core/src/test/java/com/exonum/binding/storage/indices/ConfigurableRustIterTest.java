package com.exonum.binding.storage.indices;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.database.ViewModificationCounter;
import com.google.common.collect.ImmutableList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ConfigurableRustIterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final int INITIAL_MOD_COUNT = 11;

  private static final long DEFAULT_NATIVE_HANDLE = 0x05;

  private ViewModificationCounter modCounter;

  private ConfigurableRustIter<Integer> iter;

  @Before
  public void setUp() throws Exception {
    modCounter = mock(ViewModificationCounter.class);
    when(modCounter.getModificationCount(any(View.class)))
        .thenReturn(INITIAL_MOD_COUNT);
  }

  @Test
  public void nextGoesThroughAllElements() throws Exception {
    Fork fork = createFork();
    List<Integer> underlyingList = asList(1, 2, 3);
    createFromIterable(underlyingList, fork);

    List<Integer> iterElements = ImmutableList.copyOf(new RustIterAdapter<>(iter));

    assertThat(iterElements, equalTo(underlyingList));
  }

  @Test
  public void nextIsNotAffectedByUnrelatedModifications() throws Exception {
    Snapshot view = createSnapshot();
    List<Integer> underlyingList = asList(1, 2);
    createFromIterable(underlyingList, view);

    Fork unrelatedFork = createFork();
    notifyModified(unrelatedFork);

    List<Integer> iterElements = ImmutableList.copyOf(new RustIterAdapter<>(iter));

    assertThat(iterElements, equalTo(underlyingList));
  }

  @Test
  public void nextFailsIfModifiedBeforeFirstNext() throws Exception {
    Fork fork = createFork();
    createFromIterable(emptyList(), fork);

    notifyModified(fork);

    expectedException.expect(ConcurrentModificationException.class);
    iter.next();
  }

  @Test
  public void nextFailsIfModifiedAfterFirstNext() throws Exception {
    Fork fork = createFork();
    createFromIterable(asList(1, 2), fork);

    iter.next();  // 1st must succeed

    notifyModified(fork);

    expectedException.expect(ConcurrentModificationException.class);
    iter.next();
  }

  @Test
  @Ignore // todo: decide what to do with this fucker. It used to throw ISE,
  // because AbstractNativeProxy checked that all proxies this one depends on
  // are still alive. AbstractNativeProxy2 (the second!) does not.
  //   - Throws if next throws?
  //   - Throws if view is not valid?
  public void nextFailsIfCollectionClosed() throws Exception {
    Snapshot view = createSnapshot();
    AbstractIndexProxy index = createIndex(view);

    createFromIterable(asList(1, 2), /* used to be index */null);

    view.getCleaner().close();
    expectedException.expect(IllegalStateException.class);
    iter.next();
  }

  @Test
  public void viewModificationResultsInTerminalState() throws Exception {
    Fork fork = createFork();
    createFromIterable(asList(1, 2), fork);
    try {
      notifyModified(fork);
      iter.next();  // Must throw.
      fail("Fork is modified, but view is still valid");
    } catch (ConcurrentModificationException e) {
      // Exception above is well expected.
      // Subsequent attempt to get the next item must result in the same exception:
      expectedException.expect(ConcurrentModificationException.class);
      iter.next();
    }
  }

  @Test
  public void closeDoesNotFailIfModifiedAfterTheLastNext() throws Exception {
    Fork fork = createFork();
    createFromIterable(asList(1, 2), fork);

    while (iter.next().isPresent()) {
      // skip all elements
    }

    notifyModified(fork);

    iter.close();  // It's OK to modify after the last element was retrieved.
  }

  private Fork createFork() {
    // todo: de-dupe these two?
    Cleaner unnecessaryCleaner = new Cleaner();
    return Fork.newInstance(1L, false, unnecessaryCleaner);
  }

  private Snapshot createSnapshot() {
    Cleaner unnecessaryCleaner = new Cleaner();
    return Snapshot.newInstance(2L, false, unnecessaryCleaner);
  }

  private void createFromIterable(Iterable<Integer> it, View dbView) {
    Iterator<Integer> iterator = it.iterator();
    iter = new ConfigurableRustIter<>(DEFAULT_NATIVE_HANDLE,
        (h) -> iterator.hasNext() ? iterator.next() : null,
        (h) -> { /* no-op dispose */ },
        dbView,
        modCounter);
  }

  private static AbstractIndexProxy createIndex(View view) {
    return new AbstractIndexProxy(new NativeHandle(0x01), "test_index", view) {};
  }

  private void notifyModified(Fork fork) {
    when(modCounter.isModifiedSince(eq(fork), eq(INITIAL_MOD_COUNT)))
        .thenReturn(true);
  }

}
