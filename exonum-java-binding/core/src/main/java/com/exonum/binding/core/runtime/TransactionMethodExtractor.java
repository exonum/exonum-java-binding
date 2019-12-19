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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toMap;

import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transaction.TransactionMethod;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.MessageLite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Finds and validates transaction methods in a service.
 */
final class TransactionMethodExtractor {

  /**
   * Returns a map of transaction ids to transaction methods found in a service class.
   *
   * @see TransactionMethod
   */
  static Map<Integer, TransactionMethodObject> extractTransactionMethods(Class<?> serviceClass) {
    Map<Integer, Method> transactionMethods = findTransactionMethods(serviceClass);
    Lookup lookup = MethodHandles.publicLookup()
        .in(serviceClass);
    return transactionMethods.entrySet().stream()
        .peek(tx -> validateTransactionMethod(tx.getValue(), serviceClass))
        .collect(toMap(Map.Entry::getKey,
            (e) -> toTransactionMethodObject(e.getValue(), lookup)));
  }

  @VisibleForTesting
  static Map<Integer, Method> findTransactionMethods(Class<?> serviceClass) {
    Map<Integer, Method> transactionMethods = new HashMap<>();
    while (serviceClass != Object.class) {
      Method[] classMethods = serviceClass.getDeclaredMethods();
      for (Method method : classMethods) {
        if (method.isAnnotationPresent(TransactionMethod.class)) {
          TransactionMethod annotation = method.getAnnotation(TransactionMethod.class);
          int transactionId = annotation.value();
          checkDuplicates(transactionMethods, transactionId, serviceClass, method);
          transactionMethods.put(transactionId, method);
        }
      }
      serviceClass = serviceClass.getSuperclass();
    }
    return transactionMethods;
  }

  private static void checkDuplicates(Map<Integer, Method> transactionMethods, int transactionId,
      Class<?> serviceClass, Method method) {
    if (transactionMethods.containsKey(transactionId)) {
      String firstMethodName = transactionMethods.get(transactionId).getName();
      String errorMessage = String.format("Service %s has more than one transaction with the same"
              + " id (%s): first: %s; second: %s",
          serviceClass.getName(), transactionId, firstMethodName, method.getName());
      throw new IllegalArgumentException(errorMessage);
    }
  }

  /**
   * Checks that the given transaction method signature is correct.
   */
  private static void validateTransactionMethod(Method transaction, Class<?> serviceClass) {
    String errorMessage = String.format("Method %s in a service class %s annotated with"
        + " @TransactionMethod should have precisely two parameters: transaction arguments of"
        + " 'byte[]' type or a protobuf type and transaction context of"
        + " 'com.exonum.binding.core.transaction.TransactionContext' type",
        transaction.getName(), serviceClass.getName());
    checkArgument(transaction.getParameterCount() == 2, errorMessage);
    Class<?> firstParameter = transaction.getParameterTypes()[0];
    Class<?> secondParameter = transaction.getParameterTypes()[1];
    checkArgument(firstParameter == byte[].class || isProtobufArgument(firstParameter),
        String.format(errorMessage
            + ". But first parameter type was: %s", firstParameter.getName()));
    checkArgument(TransactionContext.class.isAssignableFrom(secondParameter),
        String.format(errorMessage
            + ". But second parameter type was: %s", secondParameter.getName()));
  }

  private static TransactionMethodObject toTransactionMethodObject(Method method, Lookup lookup) {
    Serializer<?> argumentsSerializer = StandardSerializers.bytes();
    Class parameterType = method.getParameterTypes()[0];
    if (isProtobufArgument(parameterType)) {
      argumentsSerializer = StandardSerializers.protobuf(parameterType);
    }
    MethodHandle methodHandle;
    try {
      methodHandle = lookup.unreflect(method);
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException(
          String.format("Couldn't access method %s", method.getName()), e);
    }
    return new TransactionMethodObject(methodHandle, argumentsSerializer);
  }

  /**
   * Returns true if given class is a protobuf type; false otherwise.
   */
  private static boolean isProtobufArgument(Class type) {
    return MessageLite.class.isAssignableFrom(type);
  }

  private TransactionMethodExtractor() {}
}
