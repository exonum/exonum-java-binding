/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.storage.database;

import static com.exonum.binding.storage.database.ViewModificationCounter.INITIAL_COUNT;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ViewModificationCounterTest {

  private ViewModificationCounter listener;

  @BeforeEach
  void setUp() {
    listener = new ViewModificationCounter();
  }

  @Test
  void modCountShallChangeSinceNotification() {
    Fork fork = mock(Fork.class);
    Integer prevModCount = listener.getModificationCount(fork);
    listener.notifyModified(fork);

    Integer currModCount = listener.getModificationCount(fork);
    assertThat(currModCount, not(equalTo(prevModCount)));
  }

  @Test
  void snapshotShallNotBeModified() {
    Snapshot s = mock(Snapshot.class);
    assertFalse(listener.isModifiedSince(s, INITIAL_COUNT));
  }

  @Test
  void forkShallNotBeModifiedIfNoNotifications() {
    Fork fork = mock(Fork.class);
    Integer modCount = listener.getModificationCount(fork);

    assertFalse(listener.isModifiedSince(fork, modCount));
  }

  @Test
  void forkShallBeModifiedIfNotifiedExplicitGetModCount() {
    Fork fork = mock(Fork.class);
    Integer modCount = listener.getModificationCount(fork);

    listener.notifyModified(fork);
    assertTrue(listener.isModifiedSince(fork, modCount));
  }

  @Test
  void forkShallBeModifiedIfNotifiedImplicitGetModCount() {
    Fork fork = mock(Fork.class);
    listener.notifyModified(fork);

    assertTrue(listener.isModifiedSince(fork, INITIAL_COUNT));
  }

  @Test
  void forkShallNotBeModifiedIfNoNotificationsAfterModCount() {
    Fork fork = mock(Fork.class);
    listener.notifyModified(fork);

    int modCount = listener.getModificationCount(fork);
    assertFalse(listener.isModifiedSince(fork, modCount));
  }

  @Test
  void forkShallBeModifiedIfNotifiedMultipleTimes() {
    Fork fork = mock(Fork.class);
    int numModifications = 5;
    for (int i = INITIAL_COUNT; i < numModifications; i++) {
      listener.notifyModified(fork);
    }
    assertTrue(listener.isModifiedSince(fork, INITIAL_COUNT));
  }

  @Test
  void forkModificationShallNotAffectOtherFork() {
    Fork modifiedFork = mock(Fork.class);
    int modifiedModCount = listener.getModificationCount(modifiedFork);

    Fork otherFork = mock(Fork.class);
    int otherModCount = listener.getModificationCount(otherFork);

    listener.notifyModified(modifiedFork);

    assertTrue(listener.isModifiedSince(modifiedFork, modifiedModCount));
    assertFalse(listener.isModifiedSince(otherFork, otherModCount));
  }

  @Test
  void getModCountNewSnapshot() {
    Snapshot s = mock(Snapshot.class);
    assertThat(listener.getModificationCount(s), equalTo(INITIAL_COUNT));
  }

  @Test
  void getModCountNewFork() {
    Fork fork = mock(Fork.class);
    assertThat(listener.getModificationCount(fork), equalTo(INITIAL_COUNT));
  }
}
