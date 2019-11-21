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

package com.exonum.binding.core.runtime;

import static com.exonum.binding.test.Bytes.bytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.service.Service;
import com.exonum.binding.core.service.TransactionConverter;
import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transaction.TransactionExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD) // MockitoExtension is not thread-safe: see mockito/1630
class ServiceWrapperTest {

  private static final ServiceArtifactId TEST_ARTIFACT_ID =
      ServiceArtifactId.newJavaId("com.acme:foo:1.2.3");

  final ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance("test-service",
      1, TEST_ARTIFACT_ID);

  @Mock
  Service service;

  @Mock
  TransactionConverter txConverter;

  @Mock
  Node node;

  ServiceWrapper serviceWrapper;

  @BeforeEach
  void setUp() {
    serviceWrapper = new ServiceWrapper(service, txConverter, instanceSpec, node);
  }

  @Test
  void executeTransaction() throws TransactionExecutionException {
    int txId = 2;
    byte[] arguments = bytes(1, 2, 3);
    Transaction executableTx = mock(Transaction.class);

    when(txConverter.toTransaction(txId, arguments))
        .thenReturn(executableTx);

    TransactionContext context = mock(TransactionContext.class);
    serviceWrapper.executeTransaction(txId, arguments, context);

    verify(txConverter).toTransaction(txId, arguments);
    verify(executableTx).execute(context);
  }

  @Test
  void executeInvalidTransaction() {
    int txId = 2;
    byte[] arguments = bytes(1, 2, 3);

    doThrow(IllegalArgumentException.class)
        .when(txConverter)
        .toTransaction(txId, arguments);

    TransactionContext context = mock(TransactionContext.class);
    assertThrows(IllegalArgumentException.class,
        () -> serviceWrapper.executeTransaction(txId, arguments, context));
  }

  @Test
  void executeThrowingTransaction() throws TransactionExecutionException {
    int txId = 2;
    byte[] arguments = bytes(1, 2, 3);
    Transaction executableTx = mock(Transaction.class);
    when(txConverter.toTransaction(txId, arguments))
        .thenReturn(executableTx);

    TransactionExecutionException e = new TransactionExecutionException((byte) 1);
    TransactionContext context = mock(TransactionContext.class);
    doThrow(e)
        .when(executableTx)
        .execute(context);

    TransactionExecutionException actual = assertThrows(TransactionExecutionException.class,
        () -> serviceWrapper.executeTransaction(txId, arguments, context));

    assertThat(actual).isSameAs(e);
  }

  @ParameterizedTest
  @CsvSource({
      "foo, foo",
      "foo-1, foo-1",
      "'foo 1', 'foo%201'",
      "foo/bar, foo%2Fbar",
      "foo/a/b, foo%2Fa%2Fb",
  })
  void serviceApiPath(String serviceName, String expectedPathFragment) {
    ServiceInstanceSpec spec = ServiceInstanceSpec.newInstance(serviceName, 1, TEST_ARTIFACT_ID);
    serviceWrapper = new ServiceWrapper(service, txConverter, spec, node);

    assertThat(serviceWrapper.getPublicApiRelativePath()).isEqualTo(expectedPathFragment);
  }
}
