/*
 * Copyright 2019 The Exonum Team
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IncrementalModificationCounterTest {

  private IncrementalModificationCounter counter;

  @Test
  void isNotModifiedSinceInitialization() {
    counter = new IncrementalModificationCounter();
    int currentValue = counter.getCurrentValue();
    assertFalse(counter.isModifiedSince(currentValue));
  }

  @Test
  void modifiedAfterEachEvent() {
    int numEvents = 3;
    counter = new IncrementalModificationCounter();
    int initialValue = counter.getCurrentValue();
    for (int i = 0; i < numEvents; i++) {
      int prevValue = counter.getCurrentValue();
      counter.notifyModified();
      assertTrue(counter.isModifiedSince(initialValue), "Modified since initial value");
      assertTrue(counter.isModifiedSince(prevValue), "Modified since the previous value");
    }
  }
}
