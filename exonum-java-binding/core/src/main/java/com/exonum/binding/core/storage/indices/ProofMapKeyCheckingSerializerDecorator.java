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

package com.exonum.binding.core.storage.indices;

import static com.exonum.binding.core.storage.indices.StoragePreconditions.checkProofKey;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.serialization.Serializer;

/**
 * A serializer decorator that checks proof map keys for correctness.
 *
 * @see StoragePreconditions#checkProofKey(byte[])
 */
final class ProofMapKeyCheckingSerializerDecorator<T> implements Serializer<T> {

  private final Serializer<T> delegate;

  /**
   * Creates a proof map key checking serializer decorator. Will not decorate itself.
   *
   * @param serializer a serializer to decorate
   */
  public static <T> ProofMapKeyCheckingSerializerDecorator<T> from(Serializer<T> serializer) {
    if (serializer instanceof ProofMapKeyCheckingSerializerDecorator) {
      return (ProofMapKeyCheckingSerializerDecorator<T>) serializer;
    }
    return new ProofMapKeyCheckingSerializerDecorator<>(serializer);
  }

  private ProofMapKeyCheckingSerializerDecorator(Serializer<T> delegate) {
    this.delegate = checkNotNull(delegate);
  }

  @Override
  public byte[] toBytes(T proofKey) {
    byte[] dbValue = delegate.toBytes(proofKey);
    return checkProofKey(dbValue);
  }

  @Override
  public T fromBytes(byte[] serializedProofKey) {
    checkProofKey(serializedProofKey);
    return delegate.fromBytes(serializedProofKey);
  }
}
