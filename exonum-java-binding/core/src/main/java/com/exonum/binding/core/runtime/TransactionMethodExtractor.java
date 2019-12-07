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

import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transaction.TransactionMethod;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
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
  static Map<Integer, MethodHandle> extractTransactionMethods(Class<?> serviceClass) {
    Map<Integer, MethodHandle> transactions = new HashMap<>();
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    while (serviceClass != Object.class) {
      Method[] classMethods = serviceClass.getDeclaredMethods();
      for (Method method: classMethods) {
        if (method.isAnnotationPresent(TransactionMethod.class)) {
          TransactionMethod annotation = method.getAnnotation(TransactionMethod.class);
          int transactionId = annotation.id();
          validateTransactionMethod(method, serviceClass, transactions, transactionId);
          MethodHandle methodHandle;
          try {
            methodHandle = lookup.unreflect(method);
          } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(
                String.format("Couldn't access method %s", method.getName()), e);
          }
          transactions.put(transactionId, methodHandle);
        }
      }
      serviceClass = serviceClass.getSuperclass();
    }
    return transactions;
  }

  /**
   * Checks that the given transaction method signature is correct.
   */
  private static void validateTransactionMethod(Method transaction, Class<?> serviceClass,
      Map<Integer, MethodHandle> transactions, int transactionId) {
    checkArgument(!transactions.containsKey(transactionId),
        "Service %s had more than one transaction with id: %s", serviceClass.getName(),
        transactionId);

    String errorMessage = String.format("Method %s in a service class %s annotated with"
        + " @TransactionMethod should have precisely two parameters of the following types:"
        + " \"byte[]\" and \"com.exonum.binding.core.transaction.TransactionContext\"",
        transaction.getName(), serviceClass.getName());
    checkArgument(transaction.getParameterCount() == 2, errorMessage);
    for (Class<?> parameterType: transaction.getParameterTypes()) {
      checkArgument(isParameterTypeValid(parameterType), errorMessage);
    }
  }

  /**
   * Returns true if the parameter is either a byte array of an object of a class that implements
   * TransactionContext; false otherwise.
   */
  private static boolean isParameterTypeValid(Class<?> parameterType) {
    return parameterType == byte[].class
        || TransactionContext.class.isAssignableFrom(parameterType);
  }

  private TransactionMethodExtractor() {}
}
