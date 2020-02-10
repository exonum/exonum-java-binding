/*
 * Copyright 2020 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.core.storage.indices;

import com.exonum.binding.common.serialization.CheckingSerializerDecorator;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.storage.database.AbstractAccess;
import java.util.NoSuchElementException;
import java.util.Optional;

abstract class AbstractEntryIndexProxy<T> extends AbstractIndexProxy implements EntryIndex<T> {

  protected final CheckingSerializerDecorator<T> serializer;

  /**
   * Creates a new index.
   *
   * <p>Subclasses shall create a native object and pass a native handle to this constructor.
   *
   * @param nativeHandle a native handle of the created index
   * @param address the address of this index
   * @param access a database access from which the index has been created
   * @param serializer the element serializer
   * @throws NullPointerException if any parameter is null
   */
  AbstractEntryIndexProxy(NativeHandle nativeHandle, IndexAddress address, AbstractAccess access,
      CheckingSerializerDecorator<T> serializer) {
    super(nativeHandle, address, access);
    this.serializer = serializer;
  }

  @Override
  public void set(T value) {
    notifyModified();
    byte[] valueBytes = serializer.toBytes(value);
    nativeSet(getNativeHandle(), valueBytes);
  }

  @Override
  public boolean isPresent() {
    return nativeIsPresent(getNativeHandle());
  }

  @Override
  public T get() {
    byte[] value = nativeGet(getNativeHandle());
    if (value == null) {
      throw new NoSuchElementException("No value in this entry");
    }
    return serializer.fromBytes(value);
  }

  @Override
  public void remove() {
    notifyModified();
    nativeRemove(getNativeHandle());
  }

  @Override
  public Optional<T> toOptional() {
    if (isPresent()) {
      return Optional.of(get());
    } else {
      return Optional.empty();
    }
  }

  protected abstract void nativeSet(long nativeHandle, byte[] value);

  protected abstract boolean nativeIsPresent(long nativeHandle);

  protected abstract byte[] nativeGet(long nativeHandle);

  protected abstract byte[] nativeGetIndexHash(long nativeHandle);

  protected abstract void nativeRemove(long nativeHandle);
}
