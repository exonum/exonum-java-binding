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


import com.exonum.binding.common.serialization.CheckingSerializerDecorator;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.proxy.ProxyDestructor;
import com.exonum.binding.core.storage.database.AbstractAccess;
import com.exonum.binding.core.storage.database.Access;
import com.exonum.binding.core.util.LibraryLoader;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;

/**
 * A key set is an index that contains no duplicate elements (keys).
 * This implementation does not permit null elements.
 *
 * <p>The elements are stored as keys in the underlying database in the lexicographical order.
 * As each operation accepting an element needs to pass the <em>entire</em> element
 * to the underlying database as a key, it's better, in terms of performance, to use this index
 * with small elements. If you need to store large elements and can perform operations
 * by hashes of the elements, consider using a {@link ValueSetIndexProxy}.
 *
 * <p>The "destructive" methods of the set, i.e., the ones that change its contents,
 * are specified to throw {@link UnsupportedOperationException} if the set has been created with
 * a read-only database access.
 *
 * <p>All method arguments are non-null by default.
 *
 * <p>This class is not thread-safe and and its instances shall not be shared between threads.
 *
 * <p>When the access goes out of scope, this set is destroyed. Subsequent use of the closed set
 * is prohibited and will result in {@link IllegalStateException}.
 *
 * @param <E> the type of elements in this set
 * @see ValueSetIndexProxy
 * @see Access
 */
public final class KeySetIndexProxy<E> extends AbstractIndexProxy implements Iterable<E> {

  static {
    LibraryLoader.load();
  }

  // Note that we do *not* specify Spliterator.DISTINCT because it is documented in terms
  // of Object#equals which this set does not use.
  private static final int BASE_SPLITERATOR_CHARACTERISTICS =
      Spliterator.NONNULL | Spliterator.ORDERED;

  private final CheckingSerializerDecorator<E> serializer;

  /**
   * Creates a new key set proxy.
   *
   * @param address an index address
   * @param access a database access. Must be valid. If an access is read-only,
   *             "destructive" operations are not permitted.
   * @param serializer a serializer of set keys
   * @param <E> the type of keys in this set
   * @throws IllegalStateException if the access is not valid
   * @throws IllegalArgumentException if the name is empty
   * @see StandardSerializers
   */
  public static <E> KeySetIndexProxy<E> newInstance(
      IndexAddress address, AbstractAccess access, Serializer<E> serializer) {
    CheckingSerializerDecorator<E> s = CheckingSerializerDecorator.from(serializer);

    NativeHandle setNativeHandle = createNativeSet(address, access);

    return new KeySetIndexProxy<>(setNativeHandle, address, access, s);
  }

  private static NativeHandle createNativeSet(IndexAddress address, AbstractAccess access) {
    long accessNativeHandle = access.getAccessNativeHandle();
    long handle = nativeCreate(address.getName(), address.getIdInGroup().orElse(null),
        accessNativeHandle);
    NativeHandle setNativeHandle = new NativeHandle(handle);

    Cleaner cleaner = access.getCleaner();
    ProxyDestructor.newRegistered(cleaner, setNativeHandle, KeySetIndexProxy.class,
        KeySetIndexProxy::nativeFree);
    return setNativeHandle;
  }

  private KeySetIndexProxy(NativeHandle nativeHandle, IndexAddress address, AbstractAccess access,
                           CheckingSerializerDecorator<E> serializer) {
    super(nativeHandle, address, access);
    this.serializer = serializer;
  }

  /**
   * Adds a new element to the set. The method has no effect if
   * the set already contains such element.
   *
   * @param e an element to add
   * @throws IllegalStateException if this set is not valid
   * @throws UnsupportedOperationException if this set is read-only
   */
  public void add(E e) {
    notifyModified();
    byte[] dbElement = serializer.toBytes(e);
    nativeAdd(getNativeHandle(), dbElement);
  }

  /**
   * Removes all of the elements from this set.
   * The set will be empty after this method returns.
   *
   * @throws IllegalStateException if this set is not valid
   * @throws UnsupportedOperationException if this set is read-only
   */
  public void clear() {
    notifyModified();
    nativeClear(getNativeHandle());
  }
  
  /**
   * Returns true if this set contains the specified element.
   *
   * @throws IllegalStateException if this set is not valid
   */
  public boolean contains(E e) {
    byte[] dbElement = serializer.toBytes(e);
    return nativeContains(getNativeHandle(), dbElement);
  }

  /**
   * Creates an iterator over the set elements. The elements are ordered lexicographically.
   * 
   * @return an iterator over the elements of this set
   * @throws IllegalStateException if this set is not valid 
   */
  @Override
  public Iterator<E> iterator() {
    return StorageIterators.createIterator(
        nativeCreateIterator(getNativeHandle()),
        this::nativeIteratorNext,
        this::nativeIteratorFree,
        dbAccess,
        modCounter,
        serializer::fromBytes);
  }

  /**
   * Returns a stream of the set elements. The elements are ordered lexicographically.
   *
   * @throws IllegalStateException if this set is not valid
   */
  public Stream<E> stream() {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(iterator(), streamCharacteristics()),
        false);
  }

  private int streamCharacteristics() {
    if (dbAccess.canModify()) {
      return BASE_SPLITERATOR_CHARACTERISTICS;
    } else {
      return BASE_SPLITERATOR_CHARACTERISTICS | Spliterator.IMMUTABLE;
    }
  }

  /**
   * Removes the element from this set. If it's not in the set, does nothing.
   * 
   * @param e an element to remove.
   * @throws IllegalStateException if this set is not valid
   * @throws UnsupportedOperationException if this set is read-only
   */
  public void remove(E e) {
    notifyModified();
    byte[] dbElement = serializer.toBytes(e);
    nativeRemove(getNativeHandle(), dbElement);
  }

  private static native long nativeCreate(String name, @Nullable byte[] idInGroup,
      long accessNativeHandle);

  private native void nativeAdd(long nativeHandle, byte[] e);

  private native void nativeClear(long nativeHandle);

  private native boolean nativeContains(long nativeHandle, byte[] e);

  private native long nativeCreateIterator(long nativeHandle);

  private native byte[] nativeIteratorNext(long iterNativeHandle);

  private native void nativeIteratorFree(long iterNativeHandle);

  private native void nativeRemove(long nativeHandle, byte[] e);

  private static native void nativeFree(long nativeHandle);
}
