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

package com.exonum.binding.fakes.services.service;

import static com.exonum.binding.fakes.services.service.TestSchemaFactories.createTestSchemaFactory;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TestServiceTest {

  @Test
  @SuppressWarnings("unchecked") // No type parameters for clarity
  void initialize() {
    Fork fork = mock(Fork.class);
    TestSchema schema = mock(TestSchema.class);
    ProofMapIndexProxy testMap = mock(ProofMapIndexProxy.class);
    when(schema.testMap()).thenReturn(testMap);
    TestService service = new TestService(createTestSchemaFactory(fork, schema));

    Optional<String> initialConfig = service.initialize(fork);

    Optional<String> expectedConfig = Optional.of(TestService.INITIAL_CONFIGURATION);
    assertThat(initialConfig, equalTo(expectedConfig));

    verify(testMap).put(TestService.INITIAL_ENTRY_KEY, TestService.INITIAL_ENTRY_VALUE);
  }
}
