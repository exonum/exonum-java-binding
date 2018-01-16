package com.exonum.binding.fakes;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.fakes.mocks.ThrowingTransactions;
import com.exonum.binding.fakes.mocks.UserServiceAdapterMockBuilder;
import com.exonum.binding.fakes.services.service.TestSchema;
import com.exonum.binding.fakes.services.service.TestService;
import com.exonum.binding.fakes.services.transactions.SetEntryTransaction;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.service.adapters.UserServiceAdapter;
import com.exonum.binding.service.adapters.UserTransactionAdapter;
import com.exonum.binding.transport.Server;
import com.exonum.binding.util.LibraryLoader;
import io.vertx.ext.web.Router;

/**
 * Provides methods to create mocks and test fakes of Service and Transaction adapters.
 *
 * <p>This class is a one stop place to </p>
 */
@SuppressWarnings({"unused", "WeakerAccess"}) // Used in native code
public final class NativeFacade {

  static {
    LibraryLoader.load();
  }

  /**
   * Creates a UserTransactionAdapter of a transaction that puts a given value into the storage.
   *
   * @param valid whether a transaction has to be valid (i.e., return true
   *              in its {@link Transaction#isValid()} method)
   * @param value a value to put into an entry
   * @param info a value to be returned by {@link UserTransactionAdapter#info()}
   */
  public static UserTransactionAdapter createTransaction(boolean valid,
                                                         String value,
                                                         String info) {
    SetEntryTransaction userTransaction = new SetEntryTransaction(valid, value, info);
    return new UserTransactionAdapter(userTransaction);
  }

  /**
   * Creates a UserTransactionAdapter of a transaction that throws an exception of the given type
   * in all of its methods.
   *
   * @param exceptionType a type of exception to throw
   * @throws IllegalArgumentException if exception type is un-instantiable (e.g, abstract)
   */
  public static UserTransactionAdapter createThrowingTransaction(
      Class<? extends Throwable> exceptionType) {
    Transaction transaction = ThrowingTransactions.createThrowing(exceptionType);
    return new UserTransactionAdapter(transaction);
  }

  /**
   * Creates a builder of UserServiceAdapter mocks. Use it to configure custom behaviour
   * of a system-under-test, including illegal behaviour.
   *
   * @see UserServiceAdapterMockBuilder#stateHashesThrowing(Throwable)
   * @see #createTestService()
   */
  public static UserServiceAdapterMockBuilder createServiceFakeBuilder() {
    return new UserServiceAdapterMockBuilder();
  }

  /**
   * Creates a test service.
   *
   * <p>This method creates a service, not a mock of one, therefore, use it to test
   * the storage operations and transaction conversion:
   * <ul>
   *   <li>{@link UserServiceAdapter#initalize(long)}</li>
   *   <li>{@link UserServiceAdapter#getStateHashes(long)}</li>
   *   <li>{@link UserServiceAdapter#convertTransaction(byte[])}</li>
   * </ul>
   *
   * @see #createServiceFakeBuilder()
   */
  public static UserServiceAdapter createTestService() {
    Server server = createServerMock();
    TestService service = new TestService(TestSchema::new);
    return new UserServiceAdapter(service, server);
  }

  private static Server createServerMock() {
    Server server = mock(Server.class);
    Router router = mock(Router.class);
    when(server.createRouter()).thenReturn(router);
    return server;
  }

  private NativeFacade() {}
}
