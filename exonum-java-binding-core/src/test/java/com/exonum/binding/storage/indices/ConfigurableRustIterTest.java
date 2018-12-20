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

package com.exonum.binding.storage.indices;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.database.ViewModificationCounter;
import com.google.common.collect.ImmutableList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigurableRustIterTest {

  private static final int INITIAL_MOD_COUNT = 11;

  private static final long DEFAULT_NATIVE_HANDLE = 0x05;

  private ViewModificationCounter modCounter;

  private ConfigurableRustIter<Integer> iter;

  @BeforeEach
  void setUp() {
    modCounter = mock(ViewModificationCounter.class);
    when(modCounter.getModificationCount(any(View.class)))
        .thenReturn(INITIAL_MOD_COUNT);
  }

  @Test
  void nextGoesThroughAllElements() {
    Fork fork = createFork();
    List<Integer> underlyingList = asList(1, 2, 3);
    createFromIterable(underlyingList, fork);

    List<Integer> iterElements = ImmutableList.copyOf(new RustIterAdapter<>(iter));

    assertThat(iterElements, equalTo(underlyingList));
  }

  @Test
  void nextIsNotAffectedByUnrelatedModifications() {
    Snapshot view = createSnapshot();
    List<Integer> underlyingList = asList(1, 2);
    createFromIterable(underlyingList, view);

    Fork unrelatedFork = createFork();
    notifyModified(unrelatedFork);

    List<Integer> iterElements = ImmutableList.copyOf(new RustIterAdapter<>(iter));

    assertThat(iterElements, equalTo(underlyingList));
  }

  @Test
  void nextFailsIfModifiedBeforeFirstNext() {
    Fork fork = createFork();
    createFromIterable(emptyList(), fork);

    notifyModified(fork);

    assertThrows(ConcurrentModificationException.class, () -> iter.next());
  }

  @Test
  void nextFailsIfModifiedAfterFirstNext() {
    Fork fork = createFork();
    createFromIterable(asList(1, 2), fork);

    iter.next();  // 1st must succeed

    notifyModified(fork);

    assertThrows(ConcurrentModificationException.class, () -> iter.next());
  }

  @Test
  void nextFailsIfHandleClosed() {
    Fork fork = createFork();
    NativeHandle nh = new NativeHandle(DEFAULT_NATIVE_HANDLE);
    createFromIterable(nh, asList(1, 2), fork);

    // Close the native handle.
    nh.close();

    assertThrows(IllegalStateException.class, () -> iter.next());
  }

  @Test
  void viewModificationResultsInTerminalState() {
    Fork fork = createFork();
    createFromIterable(asList(1, 2), fork);
    ConcurrentModificationException e = assertThrows(ConcurrentModificationException.class, () -> {
      notifyModified(fork);
      iter.next();
    });

    assertThrows(ConcurrentModificationException.class, () -> iter.next());
  }

  /**
   * Creates a mock of a fork.
   */
  private Fork createFork() {
    return mock(Fork.class);
  }

  /**
   * Creates a mock of a snapshot.
   */
  private Snapshot createSnapshot() {
    return mock(Snapshot.class);
  }

  private void createFromIterable(Iterable<Integer> it, View dbView) {
    NativeHandle nh = new NativeHandle(DEFAULT_NATIVE_HANDLE);
    createFromIterable(nh, it, dbView);
  }

  private void createFromIterable(NativeHandle nativeHandle, Iterable<Integer> it, View dbView) {
    Iterator<Integer> iterator = it.iterator();
    iter = new ConfigurableRustIter<>(nativeHandle,
        (h) -> iterator.hasNext() ? iterator.next() : null,
        dbView,
        modCounter);
  }

  private void notifyModified(Fork fork) {
    when(modCounter.isModifiedSince(eq(fork), eq(INITIAL_MOD_COUNT)))
        .thenReturn(true);
  }

}
