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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.service.Service;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.indices.TestProtoMessages;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transaction.TransactionMethod;
import io.vertx.ext.web.Router;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TransactionMethodExtractorTest {

  @Test
  void findTransactionMethodsValidService() throws Exception {
    Map<Integer, Method> transactions =
        TransactionMethodExtractor.findTransactionMethods(ValidService.class);
    assertThat(transactions).hasSize(1);
    Method transactionMethod =
        ValidService.class.getMethod("transactionMethod", byte[].class, TransactionContext.class);
    assertThat(singletonList(transactionMethod))
        .containsExactlyElementsOf(transactions.values());
  }

  @Test
  void duplicateTransactionIdsServiceMethodExtraction() {
    Exception e = assertThrows(IllegalArgumentException.class,
        () -> TransactionMethodExtractor
            .extractTransactionMethods(DuplicateTransactionIdsService.class));
    assertThat(e.getMessage())
        .contains(String.format("Service %s has more than one transaction with the same id (%s)",
            DuplicateTransactionIdsService.class.getName(),
            DuplicateTransactionIdsService.TRANSACTION_ID),
            "transactionMethod",
            "anotherTransactionMethod");
  }

  @Test
  void missingTransactionMethodArgumentsServiceMethodExtraction() {
    Exception e = assertThrows(IllegalArgumentException.class,
        () -> TransactionMethodExtractor
            .extractTransactionMethods(MissingTransactionMethodArgumentsService.class));
    String methodName = "transactionMethod";
    String errorMessage = String.format("Method %s in a service class %s annotated with"
            + " @TransactionMethod should have precisely two parameters: transaction arguments of"
            + " 'byte[]' type or a protobuf type and transaction context of"
            + " 'com.exonum.binding.core.transaction.TransactionContext' type",
        methodName, MissingTransactionMethodArgumentsService.class.getName());
    assertThat(e.getMessage()).contains(errorMessage);
  }

  @Test
  void invalidTransactionMethodArgumentServiceMethodExtraction() {
    Exception e = assertThrows(IllegalArgumentException.class,
        () -> TransactionMethodExtractor
            .extractTransactionMethods(InvalidTransactionMethodArgumentsService.class));
    String methodName = "transactionMethod";
    String errorMessage = String.format("Method %s in a service class %s annotated with"
            + " @TransactionMethod should have precisely two parameters: transaction arguments of"
            + " 'byte[]' type or a protobuf type and transaction context of"
            + " 'com.exonum.binding.core.transaction.TransactionContext' type"
            + ". But second parameter type was: " + String.class.getName(),
        methodName, InvalidTransactionMethodArgumentsService.class.getName());
    assertThat(e.getMessage()).contains(errorMessage);
  }

  @Test
  void duplicateTransactionMethodArgumentServiceMethodExtraction() {
    Exception e = assertThrows(IllegalArgumentException.class,
        () -> TransactionMethodExtractor
            .extractTransactionMethods(DuplicateTransactionMethodArgumentsService.class));
    String methodName = "transactionMethod";
    String errorMessage = String.format("Method %s in a service class %s annotated with"
            + " @TransactionMethod should have precisely two parameters: transaction arguments of"
            + " 'byte[]' type or a protobuf type and transaction context of"
            + " 'com.exonum.binding.core.transaction.TransactionContext' type"
            + ". But second parameter type was: " + byte[].class.getName(),
        methodName, DuplicateTransactionMethodArgumentsService.class.getName());
    assertThat(e.getMessage()).contains(errorMessage);
  }

  @Test
  void findMethodsValidServiceInterfaceImplementation() throws Exception {
    Map<Integer, Method> transactions =
        TransactionMethodExtractor.findTransactionMethods(
            ValidServiceInterfaceImplementation.class);
    assertThat(transactions).hasSize(2);
    Method transactionMethod = ValidServiceInterfaceImplementation.class.getMethod(
        "transactionMethod", byte[].class, TransactionContext.class);
    Method transactionMethod2 = ValidServiceInterfaceImplementation.class.getMethod(
        "transactionMethod2", byte[].class, TransactionContext.class);
    List<Method> actualMethods = Arrays.asList(transactionMethod, transactionMethod2);
    assertThat(actualMethods).containsExactlyInAnyOrderElementsOf(transactions.values());
  }

  @Test
  void findTransactionMethodsValidServiceProtobufArguments() throws Exception {
    Map<Integer, Method> transactions =
        TransactionMethodExtractor.findTransactionMethods(ValidServiceProtobufArgument.class);
    assertThat(transactions).hasSize(1);
    Method transactionMethod =
        ValidServiceProtobufArgument.class.getMethod("transactionMethod",
            TestProtoMessages.Point.class, TransactionContext.class);
    assertThat(singletonList(transactionMethod))
        .containsExactlyElementsOf(transactions.values());
  }

  static class BasicService implements Service {

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

  static class ValidService extends BasicService {

    @TransactionMethod(TRANSACTION_ID)
    @SuppressWarnings("WeakerAccess") // Should be accessible
    public void transactionMethod(byte[] arguments, TransactionContext context) {}
  }

  static class DuplicateTransactionIdsService extends BasicService {

    @TransactionMethod(TRANSACTION_ID)
    public void transactionMethod(byte[] arguments, TransactionContext context) {}

    @TransactionMethod(TRANSACTION_ID)
    public void anotherTransactionMethod(byte[] arguments, TransactionContext context) {}
  }

  static class MissingTransactionMethodArgumentsService extends BasicService {

    @TransactionMethod(TRANSACTION_ID)
    public void transactionMethod(byte[] arguments) {}
  }

  static class InvalidTransactionMethodArgumentsService extends BasicService {

    @TransactionMethod(TRANSACTION_ID)
    public void transactionMethod(byte[] arguments, String invalidArgument) {}
  }

  static class DuplicateTransactionMethodArgumentsService extends BasicService {

    @TransactionMethod(TRANSACTION_ID)
    public void transactionMethod(byte[] arguments, byte[] context) {}
  }

  interface ServiceInterface {
    int TRANSACTION_ID = 1;

    @TransactionMethod(TRANSACTION_ID)
    void transactionMethod(byte[] arguments, TransactionContext context);
  }

  static class ValidServiceInterfaceImplementation implements ServiceInterface {

    static final int TRANSACTION_ID_2 = 2;

    @TransactionMethod(TRANSACTION_ID)
    public void transactionMethod(byte[] arguments, TransactionContext context) {}

    @TransactionMethod(TRANSACTION_ID_2)
    @SuppressWarnings("WeakerAccess") // Should be accessible
    public void transactionMethod2(byte[] arguments, TransactionContext context) {}
  }

  static class ValidServiceProtobufArgument extends BasicService {

    @TransactionMethod(TRANSACTION_ID)
    public void transactionMethod(TestProtoMessages.Point arguments, TransactionContext context) {}
  }
}
