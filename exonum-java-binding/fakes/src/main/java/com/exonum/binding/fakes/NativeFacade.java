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

package com.exonum.binding.fakes;

import static org.mockito.Mockito.mock;

import com.exonum.binding.fakes.mocks.ThrowingTransactions;
import com.exonum.binding.fakes.mocks.UserServiceAdapterMockBuilder;
import com.exonum.binding.fakes.services.ServiceArtifacts;
import com.exonum.binding.fakes.services.service.TestService;
import com.exonum.binding.fakes.services.transactions.SetEntryTransaction;
import com.exonum.binding.service.adapters.UserServiceAdapter;
import com.exonum.binding.service.adapters.UserTransactionAdapter;
import com.exonum.binding.service.adapters.ViewFactory;
import com.exonum.binding.service.adapters.ViewProxyFactory;
import com.exonum.binding.transaction.Transaction;
import com.exonum.binding.transport.Server;
import com.exonum.binding.util.LibraryLoader;
import java.io.IOException;
import java.nio.file.Paths;
import javax.annotation.Nullable;

/**
 * Provides methods to create mocks and test fakes of Service and Transaction adapters.
 *
 * <p>This class is a one stop place to </p>
 */
@SuppressWarnings({"unused", "WeakerAccess"}) // Used in native code
public final class NativeFacade {

  static {
    // Load the native library early when this class is used in native integration tests.
    LibraryLoader.load();
  }

  private static final ViewFactory VIEW_FACTORY = ViewProxyFactory.getInstance();

  /**
   * Creates a UserTransactionAdapter of a transaction that puts a given value into the storage.
   *
   * @param value a value to put into an entry
   * @param info a value to be returned by {@link UserTransactionAdapter#info()}
   */
  public static UserTransactionAdapter createTransaction(String value, String info) {
    SetEntryTransaction userTransaction = new SetEntryTransaction(value, info);
    return new UserTransactionAdapter(userTransaction, VIEW_FACTORY);
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
    return new UserTransactionAdapter(transaction, VIEW_FACTORY);
  }

  /**
   * Creates a UserTransactionAdapter that contains a transaction that throws
   * {@link com.exonum.binding.transaction.TransactionExecutionException} in its execute method.
   *
   * @param isSubclass whether method should produce a subclass of TransactionExecutionException
   * @param errorCode an error code that will be included in the exception
   * @param description a description; may be {@code null}
   * @return a transaction mock throwing in execute
   */
  public static UserTransactionAdapter createThrowingExecutionExceptionTransaction(
          boolean isSubclass,
          byte errorCode,
          @Nullable String description) {
    Transaction transaction = ThrowingTransactions
            .createThrowingExecutionException(isSubclass, errorCode, description);
    return new UserTransactionAdapter(transaction, VIEW_FACTORY);
  }

  /**
   * Creates a builder of UserServiceAdapter mocks. Use it to configure custom behaviour
   * of a system-under-test, including illegal behaviour.
   *
   * @see UserServiceAdapterMockBuilder#stateHashesThrowing(Class)
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
   *   <li>{@link UserServiceAdapter#initialize(long)}</li>
   *   <li>{@link UserServiceAdapter#getStateHashes(long)}</li>
   *   <li>{@link UserServiceAdapter#convertTransaction(short, byte[])}</li>
   * </ul>
   *
   * @see #createServiceFakeBuilder()
   */
  public static UserServiceAdapter createTestService() {
    Server server = createServerMock();
    TestService service = new TestService();
    return new UserServiceAdapter(service, server, VIEW_FACTORY);
  }

  private static Server createServerMock() {
    return mock(Server.class);
  }

  /**
   * Writes a valid service artifact to the specified location. A valid service artifact
   * can be loaded by the {@link com.exonum.binding.runtime.ServiceRuntime} and
   * the service can be instantiated.
   * @param path a path to write the artifact to
   * @throws IOException if it is unable to write the JAR to the given location
   */
  public static void createValidServiceArtifact(String path) throws IOException {
    ServiceArtifacts.createValidArtifact(Paths.get(path));
  }

  /**
   * Writes a service artifact that cannot be loaded. Such artifact will cause an exception
   * during an attempt
   * to {@linkplain com.exonum.binding.runtime.ServiceRuntime#loadArtifact(String) load} it.
   * @param path a path to write the artifact to
   * @throws IOException if it is unable to write the JAR to the given location
   */
  public static void createUnloadableServiceArtifact(String path) throws IOException {
    ServiceArtifacts.createUnloadableArtifact(Paths.get(path));
  }

  /**
   * Writes a service artifact that can be loaded, but with a service that cannot be
   * {@linkplain com.exonum.binding.runtime.ServiceRuntime#createService(String) instantiated}.
   * @param path a path to write the artifact to
   * @throws IOException if it is unable to write the JAR to the given location
   */
  public static void createServiceArtifactWithNonInstantiableService(String path)
      throws IOException {
    ServiceArtifacts.createWithUninstantiableService(Paths.get(path));
  }

  private NativeFacade() {}
}
