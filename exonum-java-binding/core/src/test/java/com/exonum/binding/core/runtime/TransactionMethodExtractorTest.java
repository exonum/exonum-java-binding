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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.service.Service;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transaction.TransactionMethod;
import io.vertx.ext.web.Router;
import org.junit.jupiter.api.Test;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class TransactionMethodExtractorTest {

  @Test
  void validServiceMethodExtraction() throws Exception {
    Map<Integer, MethodHandle> transactions =
        TransactionMethodExtractor.extractTransactionMethods(ValidService.class);
    Method transactionMethod =
        ValidService.class.getMethod("transactionMethod", byte[].class, TransactionContext.class);
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    MethodHandle methodHandle = lookup.unreflect(transactionMethod);
    // TODO: improve validating method handlers equality
    assertThat(transactions).containsExactly(entry(ValidService.TRANSACTION_ID, methodHandle));
  }

  @Test
  void duplicateTransactionIdsServiceMethodExtraction() {
    Exception e = assertThrows(IllegalArgumentException.class,
        () -> TransactionMethodExtractor
            .extractTransactionMethods(DuplicateTransactionIdsService.class));
    assertThat(e.getMessage())
        .contains(String.format("Service %s had more than one transaction with id: %s",
            DuplicateTransactionIdsService.class.getName(),
            DuplicateTransactionIdsService.TRANSACTION_ID));
  }

  @Test
  void missingTransactionMethodArgumentsServiceMethodExtraction() throws Exception {
    Exception e = assertThrows(IllegalArgumentException.class,
        () -> TransactionMethodExtractor
            .extractTransactionMethods(MissingTransactionMethodArgumentsService.class));
    Method transactionMethod =
        MissingTransactionMethodArgumentsService.class.getMethod("transactionMethod",
            byte[].class);
    String errorMessage = String.format("Method %s in a service class %s annotated with"
            + " @TransactionMethod should have precisely two parameters of the following types:"
            + " \"byte[]\" and \"com.exonum.binding.core.transaction.TransactionContext\"",
        transactionMethod.getName(), MissingTransactionMethodArgumentsService.class.getName());
    assertThat(e.getMessage()).contains(errorMessage);
  }

  @Test
  void invalidTransactionMethodArgumentServiceMethodExtraction() throws Exception {
    Exception e = assertThrows(IllegalArgumentException.class,
        () -> TransactionMethodExtractor
            .extractTransactionMethods(InvalidTransactionMethodArgumentsService.class));
    Method transactionMethod =
        InvalidTransactionMethodArgumentsService.class.getMethod("transactionMethod", byte[].class, String.class);
    String errorMessage = String.format("Method %s in a service class %s annotated with"
            + " @TransactionMethod should have precisely two parameters of the following types:"
            + " \"byte[]\" and \"com.exonum.binding.core.transaction.TransactionContext\"",
        transactionMethod.getName(), InvalidTransactionMethodArgumentsService.class.getName());
    assertThat(e.getMessage()).contains(errorMessage);
  }
}

class BasicService implements Service {

  static final int TRANSACTION_ID = 1;

  @Override
  public List<HashCode> getStateHashes(Snapshot snapshot) {
    return Collections.emptyList();
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    // no-op
  }
}

class ValidService extends BasicService {

  @TransactionMethod(id = TRANSACTION_ID)
  @SuppressWarnings("WeakerAccess") // Should be accessible
  public void transactionMethod(byte[] arguments, TransactionContext context) {}
}

class DuplicateTransactionIdsService extends BasicService {

  @TransactionMethod(id = TRANSACTION_ID)
  public void transactionMethod(byte[] arguments, TransactionContext context) {}

  @TransactionMethod(id = TRANSACTION_ID)
  public void anotherTransactionMethod(byte[] arguments, TransactionContext context) {}
}

class MissingTransactionMethodArgumentsService extends BasicService {

  @TransactionMethod(id = TRANSACTION_ID)
  @SuppressWarnings("WeakerAccess") // Should be accessible
  public void transactionMethod(byte[] arguments) {}
}

class InvalidTransactionMethodArgumentsService extends BasicService {

  @TransactionMethod(id = TRANSACTION_ID)
  @SuppressWarnings("WeakerAccess") // Should be accessible
  public void transactionMethod(byte[] arguments, String invalidArgument) {}
}
