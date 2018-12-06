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

package com.exonum.binding.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class DefaultCleanActionTest {

  @Test
  void resourceTypeEmptyByDefault() {
    // Cast lambda to CleanAction.
    CleanAction<?> a = () -> { };

    // Check the resource type.
    assertThat(a.resourceType()).isEmpty();
  }


  @Test
  void from() {
    Runnable r = mock(Runnable.class);
    String expectedResourceType = "Native proxy";

    CleanAction<String> a = CleanAction.from(r, expectedResourceType);

    // Check the resource type.
    assertThat(a.resourceType()).hasValue(expectedResourceType);

    // Execute the action.
    a.clean();

    // Verify it executed the runnable.
    verify(r).run();
  }
}
