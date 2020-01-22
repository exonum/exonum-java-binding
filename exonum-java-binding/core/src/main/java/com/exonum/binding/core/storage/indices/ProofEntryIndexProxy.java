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
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.util.LibraryLoader;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * An Entry is a database index that can contain no or a single value.
 *
 * <p>An Entry is analogous to {@link java.util.Optional}, but provides modifying ("destructive")
 * operations when created with a {@link Fork}.
 * Such methods are specified to throw {@link UnsupportedOperationException} if
 * the entry is created with a {@link Snapshot} — a read-only database access.
 *
 * <p>All method arguments are non-null by default.
 *
 * <p>This class is not thread-safe and and its instances shall not be shared between threads.
 *
 * <p>When the access goes out of scope, this entry is destroyed. Subsequent use of the closed entry
 * is prohibited and will result in {@link IllegalStateException}.
 *
 * @param <T> the type of an element in this entry
 *
 * @see Access
 */
public final class ProofEntryIndexProxy<T> extends AbstractIndexProxy implements HashableIndex {

  static {
    LibraryLoader.load();
  }

  private final CheckingSerializerDecorator<T> serializer;

  /**
   * Creates a new Entry.
   *
   * @param address an index address. Must correspond to a regular index, not a group.
   *     Use MapIndex instead of groups of entries.
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
          in the interface as well. */ AbstractAccess access,
      Serializer<E> serializer) {
    checkArgument(!address.getIdInGroup().isPresent(),
        "Groups of Entries are not supported, use a ProofMapIndex instead");
    CheckingSerializerDecorator<E> s = CheckingSerializerDecorator.from(serializer);

    NativeHandle entryNativeHandle = createNativeEntry(address.getName(), access);

    return new ProofEntryIndexProxy<>(entryNativeHandle, address, access, s);
  }

  private static NativeHandle createNativeEntry(String name, AbstractAccess access) {
    long accessNativeHandle = access.getAccessNativeHandle();
    NativeHandle entryNativeHandle = new NativeHandle(nativeCreate(name, accessNativeHandle));

    Cleaner cleaner = access.getCleaner();
    ProxyDestructor.newRegistered(cleaner, entryNativeHandle, ProofEntryIndexProxy.class,
        ProofEntryIndexProxy::nativeFree);
    return entryNativeHandle;
  }

  private ProofEntryIndexProxy(NativeHandle nativeHandle, IndexAddress address, AbstractAccess access,
                               CheckingSerializerDecorator<T> serializer) {
    super(nativeHandle, address, access);
    this.serializer = serializer;
  }

  /**
   * Sets a new value of the entry, overwriting the previous value.
   *
   * @param value a value to set. Must not be null.
   * @throws UnsupportedOperationException if the entry is read-only
   * @throws IllegalStateException if the proxy is invalid
   */
  public void set(T value) {
    notifyModified();
    byte[] valueBytes = serializer.toBytes(value);
    nativeSet(getNativeHandle(), valueBytes);
  }

  /**
   * Returns true if this entry exists in the database.
   *
   * @throws IllegalStateException if the proxy is invalid.
   */
  public boolean isPresent() {
    return nativeIsPresent(getNativeHandle());
  }

  /**
   * If value is present in the entry, returns it, otherwise,
   * throws {@link NoSuchElementException}.
   *
   * @return a non-null value
   * @throws NoSuchElementException if a value is not present in the Entry
   * @throws IllegalStateException if the proxy is invalid
   * @throws IllegalArgumentException if the supplied serializer cannot decode the value
   */
  public T get() {
    byte[] value = nativeGet(getNativeHandle());
    if (value == null) {
      throw new NoSuchElementException("No value in this entry");
    }
    return serializer.fromBytes(value);
  }

  /**
   * Returns the index hash which represents the complete state of this entry.
   * Any modifications to this entry affect the index hash.
   *
   * <p>The entry index hash is computed as SHA-256 of the entry binary representation, or
   * a hash of zeroes if the entry is not set.
   *
   * @throws IllegalStateException if the proxy is invalid
   */
  @Override
  public HashCode getIndexHash() {
    return HashCode.fromBytes(nativeGetIndexHash(getNativeHandle()));
  }

  /**
   * Removes a value from this entry.
   *
   * @throws UnsupportedOperationException if the entry is read-only.
   * @throws IllegalStateException if the proxy is invalid
   */
  public void remove() {
    notifyModified();
    nativeRemove(getNativeHandle());
  }

  /**
   * Converts the entry to {@link java.util.Optional}.
   *
   * <p>Be aware that this method represents a state of the entry at the time
   * of calling. And the returned value won't reflect the entry changes:
   * <pre>
   *  {@code
   *    entry.set("foo");
   *    Optional<String> optionalEntry = entry.toOptional();
   *    entry.remove();
   *    optionalEntry.get(); // -> returns "foo"
   *  }
   * </pre>
   *
   * @return {@code Optional.of(value)} if value is present in the entry,
   *        otherwise returns {@code Optional.empty()}
   */
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
