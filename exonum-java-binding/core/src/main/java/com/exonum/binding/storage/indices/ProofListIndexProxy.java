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

import static com.exonum.binding.storage.indices.StoragePreconditions.checkElementIndex;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkIdInGroup;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkIndexName;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkPositionIndex;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.proofs.list.ListProofNode;
import com.exonum.binding.common.proofs.list.UncheckedListProof;
import com.exonum.binding.common.proofs.list.UncheckedListProofAdapter;
import com.exonum.binding.common.serialization.CheckingSerializerDecorator;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.proxy.ProxyDestructor;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.util.LibraryLoader;
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
    checkIndexName(name);
    CheckingSerializerDecorator<E> s = CheckingSerializerDecorator.from(serializer);

    long viewNativeHandle = view.getViewNativeHandle();
    NativeHandle listNativeHandle = createNativeList(view,
        () -> nativeCreate(name, viewNativeHandle));

    return new ProofListIndexProxy<>(listNativeHandle, name, view, s);
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
    checkIndexName(groupName);
    checkIdInGroup(listId);
    CheckingSerializerDecorator<E> s = CheckingSerializerDecorator.from(serializer);

    long viewNativeHandle = view.getViewNativeHandle();
    NativeHandle setNativeHandle = createNativeList(view,
        () -> nativeCreateInGroup(groupName, listId, viewNativeHandle));

    return new ProofListIndexProxy<>(setNativeHandle, groupName, view, s);
  }

  private static native long nativeCreateInGroup(String groupName, byte[] listId,
                                                 long viewNativeHandle);

  private static NativeHandle createNativeList(View view, LongSupplier nativeListConstructor) {
    NativeHandle listNativeHandle = new NativeHandle(nativeListConstructor.getAsLong());

    Cleaner cleaner = view.getCleaner();
    ProxyDestructor.newRegistered(cleaner, listNativeHandle, ProofListIndexProxy.class,
        ProofListIndexProxy::nativeFree);
    return listNativeHandle;
  }

  private ProofListIndexProxy(NativeHandle nativeHandle, String name, View view,
                              CheckingSerializerDecorator<E> serializer) {
    super(nativeHandle, name, view, serializer);
  }

  /**
   * Returns a proof that an element exists at the specified index in this list.
   *
   * @param index the element index
   * @throws IndexOutOfBoundsException if the index is invalid
   * @throws IllegalStateException if this list is not valid
   */
  public UncheckedListProof getProof(long index) {
    checkElementIndex(index, size());

    ListProofNode listProofNode = nativeGetProof(getNativeHandle(), index);
    return new UncheckedListProofAdapter<>(listProofNode, this.serializer);
  }

  private native ListProofNode nativeGetProof(long nativeHandle, long index);

  /**
   * Returns a proof that some elements exist in the specified range in this list.
   *
   * @param from the index of the first element
   * @param to the index after the last element
   * @throws IndexOutOfBoundsException if the range is not valid
   * @throws IllegalStateException if this list is not valid
   */
  public UncheckedListProof getRangeProof(long from, long to) {
    long size = size();
    ListProofNode listProofNode = nativeGetRangeProof(getNativeHandle(),
        checkElementIndex(from, size),
        checkPositionIndex(to, size));

    return new UncheckedListProofAdapter<>(listProofNode, this.serializer);
  }

  private native ListProofNode nativeGetRangeProof(long nativeHandle, long from, long to);

  /**
   * Returns the root hash of the proof list.
   *
   * @throws IllegalStateException if this list is not valid
   */
  public HashCode getRootHash() {
    return HashCode.fromBytes(nativeGetRootHash(getNativeHandle()));
  }

  private native byte[] nativeGetRootHash(long nativeHandle);

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
