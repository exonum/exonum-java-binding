package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.StoragePreconditions.checkProofKey;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.storage.serialization.Serializer;

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
