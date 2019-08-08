package com.exonum.binding.core.runtime;

import static com.exonum.binding.test.Bytes.bytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD) // MockitoExtension is not thread-safe: see mockito/1630
class ServiceWrapperTest {

  final ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance("test-service",
      1, ServiceArtifactId.of("com.acme", "foo", "1.2.3"));

  @Mock
  Service service;

  @Mock
  TransactionConverter txConverter;

  ServiceWrapper serviceWrapper;

  @BeforeEach
  void setUp() {
    serviceWrapper = new ServiceWrapper(service, txConverter, instanceSpec);
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
}