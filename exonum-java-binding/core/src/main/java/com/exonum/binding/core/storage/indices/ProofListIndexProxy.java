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

import static com.exonum.binding.core.storage.indices.StoragePreconditions.checkRange;

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
import com.google.protobuf.InvalidProtocolBufferException;
import javax.annotation.Nullable;

/**
 * A proof list index proxy is a contiguous list of elements, capable of providing
 * cryptographic proofs that it contains a certain element at a particular position.
 * Non-null elements may be added to the end of the list only.
 *
 * <p>The proof list is implemented as a hash tree (Merkle tree).
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
 * @see Access
 */
public final class ProofListIndexProxy<E> extends AbstractListIndexProxy<E>
    implements ListIndex<E>, HashableIndex {

  static {
    LibraryLoader.load();
  }

  /**
   * Creates a new ProofListIndexProxy.
   *
   * <p><strong>Warning:</strong> do not invoke this method from service code, use
   * {@link Access#getProofList(IndexAddress, Serializer)}.
   *
   * @param address an index address
   * @param access a database access. Must be valid.
   *             If an access is read-only, "destructive" operations are not permitted.
   * @param serializer a serializer of elements
   * @param <E> the type of elements in this list
   * @throws IllegalStateException if the access is not valid
   * @throws IllegalArgumentException if the name is empty
   * @see StandardSerializers
   */
  public static <E> ProofListIndexProxy<E> newInstance(
      IndexAddress address, AbstractAccess access, Serializer<E> serializer) {
    CheckingSerializerDecorator<E> s = CheckingSerializerDecorator.from(serializer);

    NativeHandle listNativeHandle = createNativeList(address, access);

    return new ProofListIndexProxy<>(listNativeHandle, address,
        access, s);
  }

  private static NativeHandle createNativeList(IndexAddress address, AbstractAccess access) {
    long accessNativeHandle = access.getAccessNativeHandle();
    long handle = nativeCreate(address.getName(), address.getIdInGroup().orElse(null),
        accessNativeHandle);
    NativeHandle listNativeHandle = new NativeHandle(handle);

    Cleaner cleaner = access.getCleaner();
    ProxyDestructor.newRegistered(cleaner, listNativeHandle, ProofListIndexProxy.class,
        ProofListIndexProxy::nativeFree);
    return listNativeHandle;
  }

  private static native long nativeCreate(String name, @Nullable byte[] idInGroup,
      long accessNativeHandle);

  private ProofListIndexProxy(NativeHandle nativeHandle, IndexAddress address,
      AbstractAccess access, CheckingSerializerDecorator<E> serializer) {
    super(nativeHandle, address, access, serializer);
  }

  /**
   * Returns a proof of either existence or absence of an element at the specified index
   * in this list.
   *
   * @param index the element index
   * @throws IndexOutOfBoundsException if the index is invalid
   * @throws IllegalStateException if this list is not valid
   * @see <a href="../../blockchain/Blockchain.html#proofs">Blockchain Proofs</a>
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
      return ListProof.parseFrom(proofMessage);
    } catch (InvalidProtocolBufferException e) {
      // Must never happen with the correct native
      throw new IllegalStateException("Non-decodable list proof", e);
    }
  }

  @Override
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
