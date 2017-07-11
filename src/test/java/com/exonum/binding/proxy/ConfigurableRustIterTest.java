package com.exonum.binding.proxy;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.exonum.binding.storage.RustIterAdapter;
import com.google.common.collect.ImmutableList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    Fork.class
})
public class ConfigurableRustIterTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

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
    Fork fork = createForkMock();
    List<Integer> underlyingList = asList(1, 2, 3);
    createFromIterable(underlyingList, fork);

    List<Integer> iterElements = ImmutableList.copyOf(new RustIterAdapter<>(iter));

    assertThat(iterElements, equalTo(underlyingList));
  }

  @Test
  public void nextIsNotAffectedByUnrelatedModifications() throws Exception {
    Snapshot view = mock(Snapshot.class);
    List<Integer> underlyingList = asList(1, 2);
    createFromIterable(underlyingList, view);

    Fork unrelatedFork = createForkMock();
    notifyModified(unrelatedFork);

    List<Integer> iterElements = ImmutableList.copyOf(new RustIterAdapter<>(iter));

    assertThat(iterElements, equalTo(underlyingList));
  }

  @Test
  public void nextFailsIfModifiedBeforeFirstNext() throws Exception {
    Fork fork = createForkMock();
    createFromIterable(emptyList(), fork);

    notifyModified(fork);

    exception.expect(ConcurrentModificationException.class);
    iter.next();
  }

  @Test
  public void nextFailsIfModifiedAfterFirstNext() throws Exception {
    Fork fork = createForkMock();
    createFromIterable(asList(1, 2), fork);

    iter.next();  // 1st must succeed

    notifyModified(fork);

    exception.expect(ConcurrentModificationException.class);
    iter.next();
  }

  @Test
  public void viewModificationResultsInTerminalState() throws Exception {
    Fork fork = createForkMock();
    createFromIterable(asList(1, 2), fork);
    try {
      notifyModified(fork);
      iter.next();  // Must throw.
      fail("Fork is modified, but view is still valid");
    } catch (ConcurrentModificationException e) {
      // Exception above is well expected.
      // Subsequent attempt to get the next item must result in the same exception:
      exception.expect(ConcurrentModificationException.class);
      iter.next();
    }
  }

  @Test
  public void closeDoesNotFailIfModifiedAfterTheLastNext() throws Exception {
    Fork fork = createForkMock();
    createFromIterable(asList(1, 2), fork);

    while (iter.next().isPresent()) {
      // skip all elements
    }

    notifyModified(fork);

    iter.close();  // It's OK to modify after the last element was retrieved.
  }

  @Test
  public void closeFailsIfViewClosedBefore() throws Exception {
    Fork fork = createForkMock();
    createFromIterable(asList(1, 2), fork);

    when(fork.isValid()).thenReturn(false);
    exception.expect(IllegalStateException.class);
    iter.close();
  }

  private Fork createForkMock() {
    Fork fork = mock(Fork.class);
    when(fork.isValid()).thenReturn(true);
    return fork;
  }

  private void createFromIterable(Iterable<Integer> it, View parentView) {
    Iterator<Integer> iterator = it.iterator();
    iter = new ConfigurableRustIter<>(DEFAULT_NATIVE_HANDLE,
        (h) -> iterator.hasNext() ? iterator.next() : null,
        (h) -> { /* no-op dispose */ },
        parentView,
        modCounter);
  }

  private void notifyModified(Fork fork) {
    when(modCounter.isModifiedSince(eq(fork), eq(INITIAL_MOD_COUNT)))
        .thenReturn(true);
  }

}
