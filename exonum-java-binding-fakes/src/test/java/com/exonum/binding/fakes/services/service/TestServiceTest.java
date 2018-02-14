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
import org.junit.Test;

public class TestServiceTest {

  @Test
  @SuppressWarnings("unchecked") // No type parameters for clarity
  public void initialize() {
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
