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
import java.lang.invoke.MethodHandle;

/**
 * Stores a method handle of a transaction and a protobuf serializer in case of a protobuf type
 * transaction arguments.
 */
// TODO: rename to TransactionMethod after migration? Another name?
class TransactionMethodObject {
  private final MethodHandle methodHandle;
  private final Serializer<?> argumentsSerializer;

  TransactionMethodObject(MethodHandle methodHandle, Serializer<?> argumentsSerializer) {
    this.methodHandle = methodHandle;
    this.argumentsSerializer = argumentsSerializer;
  }

  MethodHandle getMethodHandle() {
    return methodHandle;
  }

  Object serializeArguments(byte[] arguments) {
    return argumentsSerializer.fromBytes(arguments);
  }
}
