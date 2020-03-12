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

import static com.exonum.binding.core.runtime.ServiceWrapper.APPLY_CONFIGURATION_TX_ID;
import static com.exonum.binding.core.runtime.ServiceWrapper.CONFIGURE_INTERFACE_NAME;
import static com.exonum.binding.core.runtime.ServiceWrapper.DEFAULT_INTERFACE_NAME;
import static com.exonum.binding.core.runtime.ServiceWrapper.SUPERVISOR_SERVICE_ID;
import static com.exonum.binding.core.runtime.ServiceWrapper.VERIFY_CONFIGURATION_TX_ID;
import static com.exonum.binding.test.Bytes.bytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.exonum.binding.common.crypto.CryptoFunctions.Ed25519;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.runtime.ServiceArtifactId;
import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.core.service.Configurable;
import com.exonum.binding.core.service.Configuration;
import com.exonum.binding.core.service.ExecutionContext;
import com.exonum.binding.core.service.ExecutionException;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.service.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/*
It is a unit test marked as IT because it loads classes with native methods (which, in turn,
load the native library in static initializers).
 */
@ExtendWith(MockitoExtension.class)
class ServiceWrapperIntegrationTest {

  private static final ServiceArtifactId TEST_ARTIFACT_ID =
      ServiceArtifactId.newJavaId("com.acme/foo", "1.2.3");
  private static final String TEST_SERVICE_NAME = "test-service";
  private static final int TEST_SERVICE_ID = 1;

  final ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_SERVICE_NAME,
      TEST_SERVICE_ID, TEST_ARTIFACT_ID);

  @Mock
  ConfigurableService service;

  @Mock
  TransactionInvoker txInvoker;

  @Mock
  Node node;

  ServiceWrapper serviceWrapper;

  @BeforeEach
  void setUp() {
    serviceWrapper = new ServiceWrapper(service, instanceSpec, txInvoker, node);
  }

  @Test
  void initialize() {
    ExecutionContext context = anyContext().build();
    Configuration config = new ServiceConfiguration(new byte[0]);
    serviceWrapper.initialize(context, config);
    verify(service).initialize(context, config);
  }

  @Test
  void initializePropagatesExecutionException() {
    ExecutionException e = new ExecutionException((byte) 1);
    ExecutionContext context = anyContext().build();
    Configuration config = new ServiceConfiguration(new byte[0]);
    doThrow(e).when(service).initialize(context, config);

    ExecutionException actual = assertThrows(ExecutionException.class,
        () -> serviceWrapper.initialize(context, config));
    assertThat(actual).isSameAs(e);
  }

  @Test
  void initializeWrapsRuntimeExceptions() {
    RuntimeException e = new RuntimeException("unexpected");
    ExecutionContext context = anyContext().build();
    Configuration config = new ServiceConfiguration(new byte[0]);
    doThrow(e).when(service).initialize(context, config);

    Exception actual = assertThrows(UnexpectedExecutionException.class,
        () -> serviceWrapper.initialize(context, config));
    assertThat(actual).hasCause(e);
  }

  @Test
  void resume() {
    ExecutionContext context = anyContext().build();
    byte[] arguments = new byte[0];
    serviceWrapper.resume(context, arguments);
    verify(service).resume(context, arguments);
  }

  @Test
  void resumePropagatesExecutionException() {
    ExecutionException e = new ExecutionException((byte) 1);
    ExecutionContext context = anyContext().build();
    byte[] arguments = new byte[0];
    doThrow(e).when(service).resume(context, arguments);

    ExecutionException actual = assertThrows(ExecutionException.class,
        () -> serviceWrapper.resume(context, arguments));
    assertThat(actual).isSameAs(e);
  }

  @Test
  void resumeWrapsRuntimeExceptions() {
    RuntimeException e = new RuntimeException("unexpected");
    ExecutionContext context = anyContext().build();
    byte[] arguments = new byte[0];
    doThrow(e).when(service).resume(context, arguments);

    Exception actual = assertThrows(UnexpectedExecutionException.class,
        () -> serviceWrapper.resume(context, arguments));
    assertThat(actual).hasCause(e);
  }

  @Test
  void executeTransactionDefaultInterface() {
    int txId = 2;
    byte[] arguments = bytes(1, 2, 3);

    ExecutionContext context = anyContext().build();
    serviceWrapper.executeTransaction(DEFAULT_INTERFACE_NAME, txId, arguments, 0, context);

    verify(txInvoker).invokeTransaction(txId, arguments, context);
  }

  @Test
  void executeInvalidTransaction() {
    int txId = 2;
    byte[] arguments = bytes(1, 2, 3);

    ExecutionContext context = anyContext().build();
    doThrow(ExecutionException.class)
        .when(txInvoker)
        .invokeTransaction(txId, arguments, context);

    assertThrows(ExecutionException.class,
        () -> serviceWrapper.executeTransaction(DEFAULT_INTERFACE_NAME, txId, arguments, 0,
            context));
  }

  @Test
  void executeVerifyConfiguration() {
    String interfaceName = CONFIGURE_INTERFACE_NAME;
    int txId = VERIFY_CONFIGURATION_TX_ID;
    byte[] arguments = bytes(1, 2, 3);

    ExecutionContext context = anyContext().build();

    serviceWrapper.executeTransaction(interfaceName, txId, arguments,
        SUPERVISOR_SERVICE_ID, context);

    Configuration expected = new ServiceConfiguration(arguments);
    verify(service).verifyConfiguration(context, expected);
  }

  @Test
  void executeVerifyConfigurationPropagatesExecutionException() {
    String interfaceName = CONFIGURE_INTERFACE_NAME;
    int txId = VERIFY_CONFIGURATION_TX_ID;
    byte[] arguments = bytes(1, 2, 3);

    ExecutionContext context = anyContext().build();

    ExecutionException e = new ExecutionException((byte) 0);
    Configuration config = new ServiceConfiguration(arguments);
    doThrow(e).when(service).verifyConfiguration(context, config);

    ExecutionException actual = assertThrows(ExecutionException.class,
        () -> serviceWrapper.executeTransaction(interfaceName, txId, arguments,
            SUPERVISOR_SERVICE_ID, context));
    assertThat(actual).isSameAs(e);
  }

  @Test
  void executeVerifyConfigurationWrapsRuntimeExceptions() {
    String interfaceName = CONFIGURE_INTERFACE_NAME;
    int txId = VERIFY_CONFIGURATION_TX_ID;
    byte[] arguments = bytes(1, 2, 3);

    ExecutionContext context = anyContext().build();

    RuntimeException e = new RuntimeException("unexpected");
    Configuration config = new ServiceConfiguration(arguments);
    doThrow(e).when(service).verifyConfiguration(context, config);

    Exception actual = assertThrows(UnexpectedExecutionException.class,
        () -> serviceWrapper.executeTransaction(interfaceName, txId, arguments,
            SUPERVISOR_SERVICE_ID, context));
    assertThat(actual).hasCause(e);
  }

  @Test
  void executeApplyConfiguration() {
    String interfaceName = CONFIGURE_INTERFACE_NAME;
    int txId = APPLY_CONFIGURATION_TX_ID;
    byte[] arguments = bytes(1, 2, 3);

    ExecutionContext context = anyContext().build();

    serviceWrapper.executeTransaction(interfaceName, txId, arguments,
        SUPERVISOR_SERVICE_ID, context);

    Configuration expected = new ServiceConfiguration(arguments);
    verify(service).applyConfiguration(context, expected);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 1, 2})
  void executeConfigurableOperationInvalidCallerId(int callerServiceId) {
    String interfaceName = CONFIGURE_INTERFACE_NAME;
    int txId = VERIFY_CONFIGURATION_TX_ID;
    byte[] arguments = bytes(1, 2, 3);

    ExecutionContext context = anyContext().build();

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> serviceWrapper.executeTransaction(interfaceName, txId, arguments, callerServiceId,
            context));

    assertThat(e.getMessage()).containsIgnoringCase("Invalid caller service id")
        .contains(Integer.toString(callerServiceId))
        .contains(Integer.toString(SUPERVISOR_SERVICE_ID));
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 2, 3})
  void executeUnknownConfigurableMethod(int txId) {
    String interfaceName = CONFIGURE_INTERFACE_NAME;
    byte[] arguments = bytes(1, 2, 3);

    ExecutionContext context = anyContext().build();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> serviceWrapper.executeTransaction(interfaceName, txId, arguments,
            SUPERVISOR_SERVICE_ID, context));
    assertThat(e.getMessage()).containsIgnoringCase("Unknown txId")
        .contains(Integer.toString(txId));
  }

  @Test
  void executeVerifyConfigurationUnconfigurableService() {
    Service service = mock(Service.class); // Does not implement Configurable
    serviceWrapper = new ServiceWrapper(service, instanceSpec, txInvoker, node);

    String interfaceName = CONFIGURE_INTERFACE_NAME;
    int txId = VERIFY_CONFIGURATION_TX_ID;
    byte[] arguments = bytes(1, 2, 3);
    ExecutionContext context = anyContext().build();

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> serviceWrapper.executeTransaction(interfaceName, txId, arguments,
            SUPERVISOR_SERVICE_ID, context));

    assertThat(e.getMessage()).containsIgnoringCase("Doesn't implement Configurable")
        .contains(TEST_SERVICE_NAME);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      " ",
      "exonum.Erc20",
      "TimeOracle"
  })
  void executeInvalidTransactionUnknownInterface(String interfaceName) {
    int txId = 2;
    byte[] arguments = bytes(1, 2, 3);

    ExecutionContext context = anyContext().build();
    Exception e = assertThrows(IllegalArgumentException.class,
        () -> serviceWrapper.executeTransaction(interfaceName, txId, arguments, 0, context));

    assertThat(e.getMessage()).containsIgnoringCase("Unknown interface")
        .contains(interfaceName);
  }

  @Test
  void beforeTransactions() {
    ExecutionContext context = anyContext().build();
    serviceWrapper.beforeTransactions(context);
    verify(service).beforeTransactions(context);
  }

  @Test
  void afterTransactionsPropagatesExecutionException() {
    ExecutionException e = new ExecutionException((byte) 0);
    ExecutionContext context = anyContext().build();
    doThrow(e).when(service).afterTransactions(context);

    ExecutionException actual = assertThrows(ExecutionException.class,
        () -> serviceWrapper.afterTransactions(context));
    assertThat(actual).isSameAs(e);
  }

  @Test
  void afterTransactionsKeepsRuntimeExceptionAsCause() {
    Exception e = new RuntimeException("Boom");
    ExecutionContext context = anyContext().build();
    doThrow(e).when(service).afterTransactions(context);

    Exception actual = assertThrows(UnexpectedExecutionException.class,
        () -> serviceWrapper.afterTransactions(context));
    assertThat(actual).hasCause(e);
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
    serviceWrapper = new ServiceWrapper(service, spec, txInvoker, node);

    assertThat(serviceWrapper.getPublicApiRelativePath()).isEqualTo(expectedPathFragment);
  }

  private static ExecutionContext.Builder anyContext() {
    return ExecutionContext.builder()
        .serviceName(TEST_SERVICE_NAME)
        .serviceId(TEST_SERVICE_ID)
        .authorPk(PublicKey.fromBytes(new byte[Ed25519.PUBLIC_KEY_BYTES]))
        .txMessageHash(HashCode.fromBytes(new byte[Hashing.DEFAULT_HASH_SIZE_BYTES]))
        .blockchainData(mock(BlockchainData.class));
  }

  private interface ConfigurableService extends Service, Configurable {
  }
}
