package com.exonum.binding.proxy;

import static com.exonum.binding.proxy.ViewModificationCounter.INITIAL_COUNT;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

public class ViewModificationCounterTest {

  ViewModificationCounter listener;

  @Before
  public void setUp() throws Exception {
    listener = new ViewModificationCounter();
  }

  @Test
  public void modCountShallChangeSinceNotification() throws Exception {
    Fork fork = mock(Fork.class);
    Integer prevModCount = listener.getModificationCount(fork);
    listener.notifyModified(fork);

    Integer currModCount = listener.getModificationCount(fork);
    assertThat(currModCount, not(equalTo(prevModCount)));
  }

  @Test
  public void snapshotShallNotBeModified() throws Exception {
    Snapshot s = mock(Snapshot.class);
    assertFalse(listener.isModifiedSince(s, INITIAL_COUNT));
  }

  @Test
  public void forkShallNotBeModifiedIfNoNotifications() throws Exception {
    Fork fork = mock(Fork.class);
    Integer modCount = listener.getModificationCount(fork);

    assertFalse(listener.isModifiedSince(fork, modCount));
  }

  @Test
  public void forkShallBeModifiedIfNotifiedExplicitGetModCount() throws Exception {
    Fork fork = mock(Fork.class);
    Integer modCount = listener.getModificationCount(fork);

    listener.notifyModified(fork);
    assertTrue(listener.isModifiedSince(fork, modCount));
  }

  @Test
  public void forkShallBeModifiedIfNotifiedImplicitGetModCount() throws Exception {
    Fork fork = mock(Fork.class);
    listener.notifyModified(fork);

    assertTrue(listener.isModifiedSince(fork, INITIAL_COUNT));
  }

  @Test
  public void forkShallNotBeModifiedIfNoNotificationsAfterModCount() throws Exception {
    Fork fork = mock(Fork.class);
    listener.notifyModified(fork);

    int modCount = listener.getModificationCount(fork);
    assertFalse(listener.isModifiedSince(fork, modCount));
  }

  @Test
  public void forkShallBeModifiedIfNotifiedMultipleTimes() throws Exception {
    Fork fork = mock(Fork.class);
    int numModifications = 5;
    for (int i = INITIAL_COUNT; i < numModifications; i++) {
      listener.notifyModified(fork);
    }
    assertTrue(listener.isModifiedSince(fork, INITIAL_COUNT));
  }

  @Test
  public void forkModificationShallNotAffectOtherFork() throws Exception {
    Fork modifiedFork = mock(Fork.class);
    int modifiedModCount = listener.getModificationCount(modifiedFork);

    Fork otherFork = mock(Fork.class);
    int otherModCount = listener.getModificationCount(otherFork);

    listener.notifyModified(modifiedFork);

    assertTrue(listener.isModifiedSince(modifiedFork, modifiedModCount));
    assertFalse(listener.isModifiedSince(otherFork, otherModCount));
  }

  @Test
  public void getModCountNewSnapshot() throws Exception {
    Snapshot s = mock(Snapshot.class);
    assertThat(listener.getModificationCount(s), equalTo(INITIAL_COUNT));
  }

  @Test
  public void getModCountNewFork() throws Exception {
    Fork fork = mock(Fork.class);
    assertThat(listener.getModificationCount(fork), equalTo(INITIAL_COUNT));
  }
}
