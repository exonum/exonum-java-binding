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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.storage.database.ModificationCounter;
import com.google.common.collect.ImmutableList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigurableRustIterTest {

  private static final int INITIAL_MOD_COUNT = 11;

  private static final long DEFAULT_NATIVE_HANDLE = 0x05;

  private ModificationCounter modCounter;

  private ConfigurableRustIter<Integer> iter;

  @BeforeEach
  void setUp() {
    modCounter = mock(ModificationCounter.class);
    when(modCounter.getCurrentValue())
        .thenReturn(INITIAL_MOD_COUNT);
  }

  @Test
  void nextGoesThroughAllElements() {
    List<Integer> underlyingList = asList(1, 2, 3);
    createFromIterable(underlyingList);

    List<Integer> iterElements = ImmutableList.copyOf(new RustIterAdapter<>(iter));

    assertThat(iterElements, equalTo(underlyingList));
  }

  @Test
  void nextFailsIfModifiedBeforeFirstNext() {
    createFromIterable(emptyList());

    notifyModified();

    assertThrows(ConcurrentModificationException.class, () -> iter.next());
  }

  @Test
  void nextFailsIfModifiedAfterFirstNext() {
    createFromIterable(asList(1, 2));

    iter.next();  // 1st must succeed

    notifyModified();

    assertThrows(ConcurrentModificationException.class, () -> iter.next());
  }

  @Test
  void nextFailsIfHandleClosed() {
    NativeHandle nh = new NativeHandle(DEFAULT_NATIVE_HANDLE);
    createFromIterable(nh, asList(1, 2));

    // Close the native handle.
    nh.close();

    assertThrows(IllegalStateException.class, () -> iter.next());
  }

  @Test
  void viewModificationResultsInTerminalState() {
    createFromIterable(asList(1, 2));

    notifyModified();

    // Any subsequent call to #next must throw
    assertThrows(ConcurrentModificationException.class, () -> iter.next());
    assertThrows(ConcurrentModificationException.class, () -> iter.next());
  }

  private void createFromIterable(Iterable<Integer> it) {
    NativeHandle nh = new NativeHandle(DEFAULT_NATIVE_HANDLE);
    createFromIterable(nh, it);
  }

  private void createFromIterable(NativeHandle nativeHandle, Iterable<Integer> it) {
    Iterator<Integer> iterator = it.iterator();
    iter = new ConfigurableRustIter<>(nativeHandle,
        (h) -> iterator.hasNext() ? iterator.next() : null,
        modCounter);
  }

  private void notifyModified() {
    when(modCounter.isModifiedSince(eq(INITIAL_MOD_COUNT)))
        .thenReturn(true);
  }

}
