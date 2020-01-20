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

import static com.exonum.binding.core.storage.indices.StoragePreconditions.checkIndexType;

import com.exonum.binding.common.serialization.CheckingSerializerDecorator;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.proxy.ProxyDestructor;
import com.exonum.binding.core.storage.database.AbstractAccess;
import com.exonum.binding.core.util.LibraryLoader;
import com.google.protobuf.MessageLite;
import java.util.function.LongSupplier;

/**
 * A list index proxy is a contiguous list of elements.
 * Elements may be added to or removed from the end of the list only.
 *
 * <p>This list implementation does not permit null elements.
 *
 * <p>The "destructive" methods of the list, i.e., those that change its contents,
 * are specified to throw {@link UnsupportedOperationException} if
 * this list has been created with a read-only database access.
 *
 * <p>All method arguments are non-null by default.
 *
 * <p>This class is not thread-safe and and its instances shall not be shared between threads.
 *
 * <p>When the access goes out of scope, this list is destroyed. Subsequent use of the closed list
 * is prohibited and will result in {@link IllegalStateException}.
 *
 * @param <E> the type of elements in this list
 * @see AbstractAccess
 */
public final class ListIndexProxy<E> extends AbstractListIndexProxy<E> implements ListIndex<E> {

  static {
    LibraryLoader.load();
  }

  /**
   * Creates a new ListIndexProxy storing protobuf messages.
   *
   * @param name a unique alphanumeric non-empty identifier of this list in the underlying storage:
   *             [a-zA-Z0-9_]
   * @param access a database access. Must be valid.
   *             If an access is read-only, "destructive" operations are not permitted.
   * @param elementType the class of an element-protobuf message
   * @param <E> the type of elements in this list; must be a protobuf message
   *     that has a public static {@code #parseFrom(byte[])} method
   * @throws IllegalStateException if the access is not valid
   * @throws IllegalArgumentException if the name is empty
   */
  public static <E extends MessageLite> ListIndexProxy<E> newInstance(
      String name, AbstractAccess access, Class<E> elementType) {
    return newInstance(name, access, StandardSerializers.protobuf(elementType));
  }

  /**
   * Creates a new ListIndexProxy.
   *
   * @param name a unique alphanumeric non-empty identifier of this list in the underlying storage:
   *             [a-zA-Z0-9_]
   * @param access a database access. Must be valid.
   *             If an access is read-only, "destructive" operations are not permitted.
   * @param serializer a serializer of elements
   * @param <E> the type of elements in this list
   * @throws IllegalStateException if the access is not valid
   * @throws IllegalArgumentException if the name is empty
   * @see StandardSerializers
   */
  public static <E> ListIndexProxy<E> newInstance(
      String name, AbstractAccess access, Serializer<E> serializer) {
    IndexAddress address = IndexAddress.valueOf(name);
    long accessNativeHandle = access.getAccessNativeHandle();
    LongSupplier nativeListConstructor = () -> nativeCreate(name, accessNativeHandle);

    return getOrCreate(address, access, serializer, nativeListConstructor);
  }

  /**
   * Creates a new list in a <a href="package-summary.html#families">collection group</a>
   * with the given name.
   *
   * <p>See a <a href="package-summary.html#families-limitations">caveat</a> on index identifiers.
   *
   * @param groupName a name of the collection group
   * @param listId an identifier of this collection in the group, see the caveats
   * @param access a database access
   * @param serializer a serializer of list elements
   * @param <E> the type of elements in this list
   * @return a new list proxy
   * @throws IllegalStateException if the access is not valid
   * @throws IllegalArgumentException if the name or index id is empty
   * @see StandardSerializers
   */
  public static <E> ListIndexProxy<E> newInGroupUnsafe(String groupName, byte[] listId,
                                                       AbstractAccess access, Serializer<E> serializer) {
    IndexAddress address = IndexAddress.valueOf(groupName, listId);
    long accessNativeHandle = access.getAccessNativeHandle();
    LongSupplier nativeListConstructor =
        () -> nativeCreateInGroup(groupName, listId, accessNativeHandle);

    return getOrCreate(address, access, serializer, nativeListConstructor);
  }

  private static <E> ListIndexProxy<E> getOrCreate(IndexAddress address, AbstractAccess access,
      Serializer<E> serializer, LongSupplier nativeListConstructor) {
    return access.findOpenIndex(address)
        .map(ListIndexProxy::<E>checkCachedInstance)
        .orElseGet(() -> newListIndexProxy(address, access, serializer, nativeListConstructor));
  }

  @SuppressWarnings("unchecked") // The compiler is correct: the cache is not type-safe: ECR-3387
  private static <E> ListIndexProxy<E> checkCachedInstance(StorageIndex cachedIndex) {
    checkIndexType(cachedIndex, ListIndexProxy.class);
    return (ListIndexProxy<E>) cachedIndex;
  }

  private static <E> ListIndexProxy<E> newListIndexProxy(IndexAddress address, AbstractAccess access,
      Serializer<E> serializer, LongSupplier nativeSetConstructor) {
    CheckingSerializerDecorator<E> s = CheckingSerializerDecorator.from(serializer);

    NativeHandle listNativeHandle = createNativeList(access, nativeSetConstructor);

    ListIndexProxy<E> list = new ListIndexProxy<>(listNativeHandle, address, access, s);
    access.registerIndex(list);
    return list;
  }

  private static NativeHandle createNativeList(AbstractAccess access, LongSupplier nativeListConstructor) {
    NativeHandle listNativeHandle = new NativeHandle(nativeListConstructor.getAsLong());

    Cleaner cleaner = access.getCleaner();
    ProxyDestructor.newRegistered(cleaner, listNativeHandle, ListIndexProxy.class,
        ListIndexProxy::nativeFree);
    return listNativeHandle;
  }

  private ListIndexProxy(NativeHandle nativeHandle, IndexAddress address, AbstractAccess access,
                         CheckingSerializerDecorator<E> serializer) {
    super(nativeHandle, address, access, serializer);
  }

  private static native long nativeCreate(String listName, long accessNativeHandle);

  private static native long nativeCreateInGroup(String groupName, byte[] listId,
                                                 long accessNativeHandle);

  private static native void nativeFree(long nativeHandle);

  @Override
  native void nativeAdd(long nativeHandle, byte[] e);

  @Override
  native void nativeSet(long nativeHandle, long index, byte[] e);

  @Override
  native byte[] nativeGet(long nativeHandle, long index);

  @Override
  native byte[] nativeGetLast(long nativeHandle);

  @Override
  native byte[] nativeRemoveLast(long nativeHandle);

  @Override
  native void nativeTruncate(long nativeHandle, long newSize);

  @Override
  native void nativeClear(long nativeHandle);

  @Override
  native boolean nativeIsEmpty(long nativeHandle);

  @Override
  native long nativeSize(long nativeHandle);

  @Override
  native long nativeCreateIter(long nativeHandle);

  @Override
  native byte[] nativeIterNext(long iterNativeHandle);

  @Override
  native void nativeIterFree(long iterNativeHandle);
}
