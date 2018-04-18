package com.exonum.binding.storage.indices;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.storage.database.ViewProxy;
import com.exonum.binding.storage.serialization.Serializer;
import com.exonum.binding.storage.serialization.StandardSerializers;

final class IndexConstructors {

  static <IndexT> PartiallyAppliedIndexConstructor<IndexT> from(
      IndexConstructorOne<IndexT, String> constructor) {
    return (name, view) -> constructor.create(name, view,
        StandardSerializers.string()
    );
  }

  static <IndexT> PartiallyAppliedIndexConstructor<IndexT> from(
      IndexConstructorTwo<IndexT, HashCode, String> constructor) {
    return (name, view) -> constructor.create(name, view,
        StandardSerializers.hash(), StandardSerializers.string()
    );
  }

  @FunctionalInterface
  interface IndexConstructorOne<IndexT, ElementT> {
    IndexT create(String name, ViewProxy view, Serializer<ElementT> serializer);
  }

  @FunctionalInterface
  interface IndexConstructorTwo<IndexT, KeyT, ValueT> {
    IndexT create(String name, ViewProxy view, Serializer<KeyT> keySerializer,
                  Serializer<ValueT> valueSerializer);
  }

  @FunctionalInterface
  interface PartiallyAppliedIndexConstructor<IndexT> {
    IndexT create(String name, ViewProxy view);
  }

  private IndexConstructors() {}
}
