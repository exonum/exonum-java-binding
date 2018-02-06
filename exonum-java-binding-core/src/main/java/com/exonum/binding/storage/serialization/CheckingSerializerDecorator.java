package com.exonum.binding.storage.serialization;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A serializer decorator, that performs some extra checks to ensure that a user-supplied
 * decorator adheres to {@link Serializer} contract. These are required in Java code
 * that interacts with native code and accepts user-implemented serializers.
 *
 * @param <T> a type of serializable object
 */
public class CheckingSerializerDecorator<T> implements Serializer<T> {

  private final Serializer<T> delegate;

  /**
   * Creates a new decorator.
   *
   * @param delegate a serializer to decorate
   */
  public CheckingSerializerDecorator(Serializer<T> delegate) {
    this.delegate = checkNotNull(delegate);
  }

  @Override
  public byte[] toBytes(T value) {
    byte[] valueBytes = delegate.toBytes(checkNotNull(value, "value is null"));
    return checkNotNull(valueBytes,
        "Broken serializer (%s): produces null byte array for a non-null value", delegate);
  }

  @Override
  public T fromBytes(byte[] serializedValue) {
    T value = delegate.fromBytes(checkNotNull(serializedValue, "serializedValue is null"));
    return checkNotNull(value, "Broken serializer (%s): produces a null value for a non-null array."
        + " You must throw an exception if a serialized value cannot be converted "
        + "to an instance of the given type.", delegate);
  }
}
