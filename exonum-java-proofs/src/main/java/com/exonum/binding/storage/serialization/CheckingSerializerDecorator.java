/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.storage.serialization;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A serializer decorator, that performs some extra checks to ensure that a user-supplied
 * serializer adheres to {@link Serializer} contract. These are required in Java code
 * that interacts with native code and accepts user-implemented serializers.
 *
 * @implNote This class slightly breaks LSP for it throws IllegalStateException
 *     when the user-supplied serializer returns nulls (i.e., it breaks LSP iff the delegate
 *     breaks LSP).
 *
 * @param <T> a type of serializable object
 */
public final class CheckingSerializerDecorator<T> implements Serializer<T> {

  private final Serializer<T> delegate;

  /**
   * Creates a checking serializer decorator. Will not decorate itself.
   *
   * @param serializer a serializer to decorate
   */
  public static <T> CheckingSerializerDecorator<T> from(Serializer<T> serializer) {
    if (serializer instanceof CheckingSerializerDecorator) {
      return (CheckingSerializerDecorator<T>) serializer;
    }
    return new CheckingSerializerDecorator<>(serializer);
  }

  private CheckingSerializerDecorator(Serializer<T> delegate) {
    this.delegate = checkNotNull(delegate);
  }

  @Override
  public byte[] toBytes(T value) {
    byte[] valueBytes = delegate.toBytes(checkNotNull(value, "value is null"));
    checkState(valueBytes != null,
        "Broken serializer (%s): produces null byte array for a non-null value", delegate);
    return valueBytes;
  }

  @Override
  public T fromBytes(byte[] serializedValue) {
    T value = delegate.fromBytes(checkNotNull(serializedValue, "serializedValue is null"));
    checkState(value != null, "Broken serializer (%s): produces a null value for a non-null array."
        + " You must throw an exception if a serialized value cannot be converted "
        + "to an instance of the given type.", delegate);
    return value;
  }
}
