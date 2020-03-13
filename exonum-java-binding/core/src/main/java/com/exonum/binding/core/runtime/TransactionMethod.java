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

import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.core.service.ExecutionContext;
import com.exonum.binding.core.service.ExecutionException;
import com.exonum.binding.core.service.Service;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.WrongMethodTypeException;

/**
 * A proxy of a transaction method. This class implements argument resolution,
 * conversion, and invocation of a transaction method.
 */
class TransactionMethod {
  private final MethodHandle methodHandle;
  private final Serializer<?> argumentsSerializer;

  TransactionMethod(MethodHandle methodHandle, Serializer<?> argumentsSerializer) {
    this.methodHandle = methodHandle;
    this.argumentsSerializer = argumentsSerializer;
  }

  void invoke(Service targetService, byte[] arguments, ExecutionContext context) {
    Object argumentsObject = serializeArguments(arguments);
    try {
      methodHandle.invoke(targetService, argumentsObject, context);
    } catch (WrongMethodTypeException | ClassCastException invocationException) {
      // Invocation-specific exceptions are thrown as is — they are not thrown
      // from the _transaction method_, but from framework code (see mh#invoke spec).
      throw invocationException;
    } catch (ExecutionException serviceException) {
      // 'Service-defined' transaction exceptions
      throw serviceException;
    } catch (Throwable unexpectedServiceException) {
      // Any other _transaction_ exceptions
      throw new UnexpectedExecutionException(unexpectedServiceException);
    }
  }

  private Object serializeArguments(byte[] arguments) {
    return argumentsSerializer.fromBytes(arguments);
  }
}
