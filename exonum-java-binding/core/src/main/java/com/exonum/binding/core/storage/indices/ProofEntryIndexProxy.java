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

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.CheckingSerializerDecorator;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.proxy.ProxyDestructor;
import com.exonum.binding.core.storage.database.AbstractAccess;
import com.exonum.binding.core.storage.database.Access;
import com.exonum.binding.core.util.LibraryLoader;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * A proxy of a native MerkleDB ProofEntry.
 *
 * <p>This class is not thread-safe and and its instances shall not be shared between threads.
 *
 * @param <T> the type of an element in this entry
 */
public final class ProofEntryIndexProxy<T> extends AbstractIndexProxy implements
    ProofEntryIndex<T> {

  static {
    LibraryLoader.load();
  }

  private final CheckingSerializerDecorator<T> serializer;

  /**
   * Creates a new Entry.
   *
   * <p><strong>Warning:</strong> do not invoke this method from service code, use
   * {@link Access#getProofEntry(IndexAddress, Serializer)}.
   *
   * @param address an index address. Must correspond to a regular index, not a group.
   *     Use {@link ProofMapIndexProxy} instead of groups of entries.
   * @param access a database access. Must be valid.
   *     If an access is read-only, "destructive" operations are not permitted.
   * @param serializer an entry serializer
   *
   * @throws IllegalArgumentException if the name is empty
   * @throws IllegalStateException if the access proxy is invalid
   * @see StandardSerializers
   */
  public static <E> ProofEntryIndexProxy<E> newInstance(IndexAddress address,
      /* todo: (here and elsewhere) or Access? That would require pulling up #getCleaner
          in the interface as well. */
      AbstractAccess access,
      Serializer<E> serializer) {
    checkArgument(!address.getIdInGroup().isPresent(),
        "Groups of Entries are not supported, use a ProofMapIndex instead");
    CheckingSerializerDecorator<E> s = CheckingSerializerDecorator.from(serializer);

    NativeHandle entryNativeHandle = createNativeEntry(address.getName(), access);

    return new ProofEntryIndexProxy<>(entryNativeHandle, address, access, s);
  }

  private static NativeHandle createNativeEntry(String name, AbstractAccess access) {
    long accessNativeHandle = access.getAccessNativeHandle();
    long handle = nativeCreate(name, accessNativeHandle);
    NativeHandle entryNativeHandle = new NativeHandle(handle);

    Cleaner cleaner = access.getCleaner();
    ProxyDestructor.newRegistered(cleaner, entryNativeHandle, ProofEntryIndexProxy.class,
        ProofEntryIndexProxy::nativeFree);
    return entryNativeHandle;
  }

  private ProofEntryIndexProxy(NativeHandle nativeHandle, IndexAddress address,
      AbstractAccess access, CheckingSerializerDecorator<T> serializer) {
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
  public HashCode getIndexHash() {
    return HashCode.fromBytes(nativeGetIndexHash(getNativeHandle()));
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

  private static native long nativeCreate(String name, long accessNativeHandle);

  private native void nativeSet(long nativeHandle, byte[] value);

  private native boolean nativeIsPresent(long nativeHandle);

  private native byte[] nativeGet(long nativeHandle);

  private native byte[] nativeGetIndexHash(long nativeHandle);

  private native void nativeRemove(long nativeHandle);

  private static native void nativeFree(long nativeHandle);
}
