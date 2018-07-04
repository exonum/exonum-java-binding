/*
 * Copyright 2018 The Exonum Team
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

package com.exonum.binding.storage.indices;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.storage.database.View;
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
    IndexT create(String name, View view, Serializer<ElementT> serializer);
  }

  @FunctionalInterface
  interface IndexConstructorTwo<IndexT, KeyT, ValueT> {
    IndexT create(String name, View view, Serializer<KeyT> keySerializer,
                  Serializer<ValueT> valueSerializer);
  }

  @FunctionalInterface
  interface PartiallyAppliedIndexConstructor<IndexT> {
    IndexT create(String name, View view);
  }

  private IndexConstructors() {}
}
