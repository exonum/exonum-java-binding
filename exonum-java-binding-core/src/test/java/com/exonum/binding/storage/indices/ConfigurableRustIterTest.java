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

import com.exonum.binding.storage.database.ForkProxy;
import com.exonum.binding.storage.database.SnapshotProxy;
import com.exonum.binding.storage.database.ViewModificationCounter;
import com.exonum.binding.storage.database.ViewProxy;
import com.google.common.collect.ImmutableList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
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
    when(modCounter.getModificationCount(any(ViewProxy.class)))
        .thenReturn(INITIAL_MOD_COUNT);
  }

  @Test
  public void nextGoesThroughAllElements() throws Exception {
    ForkProxy fork = createFork();
    List<Integer> underlyingList = asList(1, 2, 3);
    createFromIterable(underlyingList, fork);

    List<Integer> iterElements = ImmutableList.copyOf(new RustIterAdapter<>(iter));

    assertThat(iterElements, equalTo(underlyingList));
  }

  @Test
  public void nextIsNotAffectedByUnrelatedModifications() throws Exception {
    SnapshotProxy view = createSnapshot();
    List<Integer> underlyingList = asList(1, 2);
    createFromIterable(underlyingList, view);

    ForkProxy unrelatedFork = createFork();
    notifyModified(unrelatedFork);

    List<Integer> iterElements = ImmutableList.copyOf(new RustIterAdapter<>(iter));

    assertThat(iterElements, equalTo(underlyingList));
  }

  @Test
  public void nextFailsIfModifiedBeforeFirstNext() throws Exception {
    ForkProxy fork = createFork();
    createFromIterable(emptyList(), fork);

    notifyModified(fork);

    expectedException.expect(ConcurrentModificationException.class);
    iter.next();
  }

  @Test
  public void nextFailsIfModifiedAfterFirstNext() throws Exception {
    ForkProxy fork = createFork();
    createFromIterable(asList(1, 2), fork);

    iter.next();  // 1st must succeed

    notifyModified(fork);

    expectedException.expect(ConcurrentModificationException.class);
    iter.next();
  }

  @Test
  public void nextFailsIfCollectionClosed() throws Exception {
    SnapshotProxy view = createSnapshot();
    AbstractIndexProxy index = createIndex(view);

    createFromIterable(asList(1, 2), index);

    index.close();
    expectedException.expect(IllegalStateException.class);
    iter.next();
  }

  @Test
  public void nextFailsIfViewClosed() throws Exception {
    SnapshotProxy view = createSnapshot();
    createFromIterable(asList(1, 2), view);

    view.close();
    expectedException.expect(IllegalStateException.class);
    iter.next();
  }

  @Test
  public void viewModificationResultsInTerminalState() throws Exception {
    ForkProxy fork = createFork();
    createFromIterable(asList(1, 2), fork);
    try {
      notifyModified(fork);
      iter.next();  // Must throw.
      fail("ForkProxy is modified, but view is still valid");
    } catch (ConcurrentModificationException e) {
      // Exception above is well expected.
      // Subsequent attempt to get the next item must result in the same exception:
      expectedException.expect(ConcurrentModificationException.class);
      iter.next();
    }
  }

  @Test
  public void closeDoesNotFailIfModifiedAfterTheLastNext() throws Exception {
    ForkProxy fork = createFork();
    createFromIterable(asList(1, 2), fork);

    while (iter.next().isPresent()) {
      // skip all elements
    }

    notifyModified(fork);

    iter.close();  // It's OK to modify after the last element was retrieved.
  }

  @Test
  public void closeFailsIfViewClosedBefore() throws Exception {
    ForkProxy fork = createFork();
    createFromIterable(asList(1, 2), fork);

    fork.close();
    expectedException.expect(IllegalStateException.class);
    iter.close();
  }

  private static ForkProxy createFork() {
    return new ForkProxy(1L, false);
  }

  private static SnapshotProxy createSnapshot() {
    return new SnapshotProxy(2L, false);
  }

  private void createFromIterable(Iterable<Integer> it, ViewProxy parentView) {
    AbstractIndexProxy collection = createIndex(parentView);
    createFromIterable(it, collection);
  }

  private void createFromIterable(Iterable<Integer> it, AbstractIndexProxy index) {
    Iterator<Integer> iterator = it.iterator();
    iter = new ConfigurableRustIter<>(DEFAULT_NATIVE_HANDLE,
        (h) -> iterator.hasNext() ? iterator.next() : null,
        (h) -> { /* no-op dispose */ },
        index,
        modCounter);
  }

  private static AbstractIndexProxy createIndex(ViewProxy view) {
    return new AbstractIndexProxy(0x01, "test_index", view) {

      @Override
      protected void disposeInternal() {
        // no-op
      }
    };
  }

  private void notifyModified(ForkProxy fork) {
    when(modCounter.isModifiedSince(eq(fork), eq(INITIAL_MOD_COUNT)))
        .thenReturn(true);
  }

}
