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

package com.exonum.binding.fakes.services.service;

import static com.exonum.binding.fakes.services.service.TestService.INITIAL_ENTRY_KEY;
import static com.exonum.binding.fakes.services.service.TestService.INITIAL_ENTRY_VALUE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.TemporaryDb;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.test.RequiresNativeLibrary;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class TestServiceIntegrationTest {

  @Test
  @RequiresNativeLibrary
  void initialize() throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner();
        TemporaryDb temporaryDb = TemporaryDb.newInstance()) {
      Fork fork = temporaryDb.createFork(cleaner);
      TestService service = new TestService();

      Optional<String> initialConfig = service.initialize(fork);

      ProofMapIndexProxy<HashCode, String> testMap = new TestSchema(fork).testMap();

      assertTrue(testMap.containsKey(INITIAL_ENTRY_KEY));
      assertThat(testMap.get(INITIAL_ENTRY_KEY), equalTo(INITIAL_ENTRY_VALUE));
    }
  }
}
