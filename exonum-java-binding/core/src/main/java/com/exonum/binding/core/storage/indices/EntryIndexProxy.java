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

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.common.serialization.CheckingSerializerDecorator;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.proxy.ProxyDestructor;
import com.exonum.binding.core.storage.database.AbstractAccess;
import com.exonum.binding.core.storage.database.Access;
import com.exonum.binding.core.util.LibraryLoader;

/**
 * A proxy of a native MerkleDB Entry.
 *
 * <p>This class is not thread-safe and and its instances shall not be shared between threads.
 *
 * @param <T> the type of an element in this entry
 */
public final class EntryIndexProxy<T> extends AbstractEntryIndexProxy<T> {

  static {
    LibraryLoader.load();
  }

  /**
   * Creates a new Entry.
   *
   * <p><strong>Warning:</strong> do not invoke this method from service code, use
   * {@link Access#getEntry(IndexAddress, Serializer)}.
   *
   * @param address an index address. Must correspond to a regular index, not a group.
   *     Use {@link MapIndex} instead of groups of entries.
   * @param access a database access. Must be valid.
   *     If an access is read-only, "destructive" operations are not permitted.
   * @param serializer an entry serializer
   *
   * @throws IllegalArgumentException if the name is empty
   * @throws IllegalStateException if the access proxy is invalid
   * @see StandardSerializers
   */
  public static <E> EntryIndexProxy<E> newInstance(IndexAddress address, AbstractAccess access,
      Serializer<E> serializer) {
    checkArgument(address.getIdInGroup().isEmpty(),
        "Groups of Entries are not supported, use a MapIndex instead");
    CheckingSerializerDecorator<E> s = CheckingSerializerDecorator.from(serializer);

    NativeHandle entryNativeHandle = createNativeEntry(address.getName(), access);

    return new EntryIndexProxy<>(entryNativeHandle, address, access, s);
  }

  private static NativeHandle createNativeEntry(String name, AbstractAccess access) {
    long accessNativeHandle = access.getAccessNativeHandle();
    long handle = nativeCreate(name, accessNativeHandle);
    NativeHandle entryNativeHandle = new NativeHandle(handle);

    Cleaner cleaner = access.getCleaner();
    ProxyDestructor.newRegistered(cleaner, entryNativeHandle, EntryIndexProxy.class,
        EntryIndexProxy::nativeFree);
    return entryNativeHandle;
  }

  private EntryIndexProxy(NativeHandle nativeHandle, IndexAddress address, AbstractAccess access,
      CheckingSerializerDecorator<T> serializer) {
    super(nativeHandle, address, access, serializer);
  }

  private static native long nativeCreate(String name, long accessNativeHandle);

  @Override
  protected native void nativeSet(long nativeHandle, byte[] value);

  @Override
  protected native boolean nativeIsPresent(long nativeHandle);

  @Override
  protected native byte[] nativeGet(long nativeHandle);

  @Override
  protected native byte[] nativeGetIndexHash(long nativeHandle);

  @Override
  protected native void nativeRemove(long nativeHandle);

  private static native void nativeFree(long nativeHandle);
}
