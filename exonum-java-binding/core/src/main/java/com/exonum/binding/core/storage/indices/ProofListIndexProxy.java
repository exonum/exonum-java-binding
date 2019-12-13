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
import static com.exonum.binding.core.storage.indices.StoragePreconditions.checkRange;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.CheckingSerializerDecorator;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.proxy.ProxyDestructor;
import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.core.util.LibraryLoader;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import java.util.function.LongSupplier;

/**
 * A proof list index proxy is a contiguous list of elements, capable of providing
 * cryptographic proofs that it contains a certain element at a particular position.
 * Non-null elements may be added to the end of the list only.
 *
 * <p>The proof list is implemented as a hash tree (Merkle tree).
 *
 * <p>The "destructive" methods of the list, i.e., those that change its contents,
 * are specified to throw {@link UnsupportedOperationException} if
 * this list has been created with a read-only database view.
 *
 * <p>All method arguments are non-null by default.
 *
 * <p>This class is not thread-safe and and its instances shall not be shared between threads.
 *
 * <p>When the view goes out of scope, this list is destroyed. Subsequent use of the closed list
 * is prohibited and will result in {@link IllegalStateException}.
 *
 * @param <E> the type of elements in this list
 * @see View
 */
public final class ProofListIndexProxy<E> extends AbstractListIndexProxy<E>
    implements ListIndex<E> {

  static {
    LibraryLoader.load();
  }

  /**
   * Creates a new ProofListIndexProxy storing protobuf messages.
   *
   * @param name a unique alphanumeric non-empty identifier of this list in the underlying storage:
   *             [a-zA-Z0-9_]
   * @param view a database view. Must be valid.
   *             If a view is read-only, "destructive" operations are not permitted.
   * @param elementType the class of elements-protobuf messages
   * @param <E> the type of elements in this list; must be a protobuf message
   *     that has a public static {@code #parseFrom(byte[])} method
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the name is empty
   */
  public static <E extends MessageLite> ProofListIndexProxy<E> newInstance(
      String name, View view, Class<E> elementType) {
    return newInstance(name, view, StandardSerializers.protobuf(elementType));
  }

  /**
   * Creates a new ProofListIndexProxy.
   *
   * @param name a unique alphanumeric non-empty identifier of this list in the underlying storage:
   *             [a-zA-Z0-9_]
   * @param view a database view. Must be valid.
   *             If a view is read-only, "destructive" operations are not permitted.
   * @param serializer a serializer of elements
   * @param <E> the type of elements in this list
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the name is empty
   * @see StandardSerializers
   */
  public static <E> ProofListIndexProxy<E> newInstance(
      String name, View view, Serializer<E> serializer) {
    IndexAddress address = IndexAddress.valueOf(name);
    long viewNativeHandle = view.getViewNativeHandle();
    LongSupplier nativeListConstructor = () -> nativeCreate(name, viewNativeHandle);

    return getOrCreate(address, view, serializer, nativeListConstructor);
  }

  private static native long nativeCreate(String listName, long viewNativeHandle);

  /**
   * Creates a new list in a <a href="package-summary.html#families">collection group</a>
   * with the given name.
   *
   * <p>See a <a href="package-summary.html#families-limitations">caveat</a> on index identifiers.
   *
   * @param groupName a name of the collection group
   * @param listId an identifier of this collection in the group, see the caveats
   * @param view a database view
   * @param serializer a serializer of list elements
   * @param <E> the type of elements in this list
   * @return a new list proxy
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the name or index id is empty
   * @see StandardSerializers
   */
  public static <E> ProofListIndexProxy<E> newInGroupUnsafe(String groupName, byte[] listId,
                                                            View view, Serializer<E> serializer) {
    IndexAddress address = IndexAddress.valueOf(groupName, listId);
    long viewNativeHandle = view.getViewNativeHandle();
    LongSupplier nativeListConstructor =
        () -> nativeCreateInGroup(groupName, listId, viewNativeHandle);

    return getOrCreate(address, view, serializer, nativeListConstructor);
  }

  private static native long nativeCreateInGroup(String groupName, byte[] listId,
                                                 long viewNativeHandle);

  private static <E> ProofListIndexProxy<E> getOrCreate(IndexAddress address, View view,
      Serializer<E> serializer, LongSupplier nativeListConstructor) {
    return view.findOpenIndex(address)
        .map(ProofListIndexProxy::<E>checkCachedInstance)
        .orElseGet(() -> newListIndexProxy(address, view, serializer, nativeListConstructor));
  }

  @SuppressWarnings("unchecked") // The compiler is correct: the cache is not type-safe: ECR-3387
  private static <E> ProofListIndexProxy<E> checkCachedInstance(StorageIndex cachedIndex) {
    checkIndexType(cachedIndex, ProofListIndexProxy.class);
    return (ProofListIndexProxy<E>) cachedIndex;
  }

  private static <E> ProofListIndexProxy<E> newListIndexProxy(IndexAddress address, View view,
      Serializer<E> serializer, LongSupplier nativeListConstructor) {
    CheckingSerializerDecorator<E> s = CheckingSerializerDecorator.from(serializer);

    NativeHandle listNativeHandle = createNativeList(view, nativeListConstructor);

    ProofListIndexProxy<E> list = new ProofListIndexProxy<>(listNativeHandle, address, view, s);
    view.registerIndex(list);
    return list;
  }

  private static NativeHandle createNativeList(View view, LongSupplier nativeListConstructor) {
    NativeHandle listNativeHandle = new NativeHandle(nativeListConstructor.getAsLong());

    Cleaner cleaner = view.getCleaner();
    ProxyDestructor.newRegistered(cleaner, listNativeHandle, ProofListIndexProxy.class,
        ProofListIndexProxy::nativeFree);
    return listNativeHandle;
  }

  private ProofListIndexProxy(NativeHandle nativeHandle, IndexAddress address, View view,
                              CheckingSerializerDecorator<E> serializer) {
    super(nativeHandle, address, view, serializer);
  }

  /**
   * Returns a proof of either existence or absence of an element at the specified index
   * in this list.
   *
   * @param index the element index
   * @throws IndexOutOfBoundsException if the index is invalid
   * @throws IllegalStateException if this list is not valid
   */
  public ListProof getProof(long index) {
    byte[] proofMessage = nativeGetProof(getNativeHandle(), index);
    return parseProof(proofMessage);
  }

  private native byte[] nativeGetProof(long nativeHandle, long index);

  /**
   * Returns a proof of either existence or absence of some elements in the specified range
   * in this list. If some elements are present in the list, but some — are not (i.e., the
   * requested range exceeds its size), a proof of absence is returned.
   *
   * @param from the index of the first element
   * @param to the index after the last element
   * @throws IndexOutOfBoundsException if the range is not valid
   * @throws IllegalStateException if this list is not valid
   */
  public ListProof getRangeProof(long from, long to) {
    checkRange(from, to);
    byte[] proofMessage = nativeGetRangeProof(getNativeHandle(), from, to);
    return parseProof(proofMessage);
  }

  private native byte[] nativeGetRangeProof(long nativeHandle, long from, long to);

  private static ListProof parseProof(byte[] proofMessage) {
    try {
      return ListProof.newInstance(proofMessage);
    } catch (InvalidProtocolBufferException e) {
      // Must never happen with the correct native
      throw new IllegalStateException("Non-decodable list proof", e);
    }
  }

  /**
   * Returns the index hash which represents the complete state of this list.
   * Any modifications to the stored entries affect the index hash.
   *
   * @throws IllegalStateException if this list is not valid
   */
  public HashCode getIndexHash() {
    return HashCode.fromBytes(nativeGetIndexHash(getNativeHandle()));
  }

  private native byte[] nativeGetIndexHash(long nativeHandle);

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
