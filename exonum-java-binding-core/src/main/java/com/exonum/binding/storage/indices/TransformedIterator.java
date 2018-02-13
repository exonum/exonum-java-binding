/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Modifications copyright 2016 The Exonum Team
 */

package com.exonum.binding.storage.indices;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;

/**
 * An iterator that transforms a backing iterator.
 *
 * <p>A modified copy of {@link com.google.common.collect.TransformedIterator}
 * that accepts a lambda and implements {@link StorageIterator}. Shall be removed
 * when ECR-595 is resolved.
 *
 * @author Louis Wasserman
 */
final class TransformedIterator<InT, OutT> implements StorageIterator<OutT> {
  private final StorageIterator<? extends InT> backingIterator;
  private final Function<? super InT, ? extends OutT> transformingFunction;

  TransformedIterator(StorageIterator<? extends InT> backingIterator,
                      Function<? super InT, ? extends OutT> transformingFunction) {
    this.backingIterator = checkNotNull(backingIterator);
    this.transformingFunction = checkNotNull(transformingFunction);
  }

  @Override
  public boolean hasNext() {
    return backingIterator.hasNext();
  }

  @Override
  public OutT next() {
    return transform(backingIterator.next());
  }

  private OutT transform(InT from) {
    return transformingFunction.apply(from);
  }

  @Override
  public void remove() {
    backingIterator.remove();
  }

  @Override
  public void close() {
    backingIterator.close();
  }
}
