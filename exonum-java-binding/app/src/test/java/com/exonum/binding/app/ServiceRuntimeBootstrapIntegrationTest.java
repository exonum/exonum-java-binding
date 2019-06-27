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

package com.exonum.binding.app;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.exonum.binding.core.runtime.ServiceRuntime;
import com.exonum.binding.core.storage.database.MemoryDb;
import com.exonum.binding.test.RequiresNativeLibrary;
import org.junit.jupiter.api.Test;

@RequiresNativeLibrary
class ServiceRuntimeBootstrapIntegrationTest {

  private static final int PORT = 25000;

  @Test
  void createServiceRuntime() {
    ServiceRuntime serviceRuntime = ServiceRuntimeBootstrap.createServiceRuntime(PORT);

    // Check the runtime is created
    assertNotNull(serviceRuntime);

    // Check that once createServiceRuntime returns, the native library is loaded. If it’s not,
    // we’ll get an UnsatisfiedLinkError, failing the test.
    try (MemoryDb database = MemoryDb.newInstance()) {
      assertNotNull(database);
    }
  }
}
