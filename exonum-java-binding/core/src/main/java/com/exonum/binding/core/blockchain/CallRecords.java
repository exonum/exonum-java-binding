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

package com.exonum.binding.core.blockchain;

import static com.exonum.binding.common.serialization.StandardSerializers.protobuf;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.core.proxy.AbstractNativeProxy;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.proxy.ProxyDestructor;
import com.exonum.binding.core.storage.database.AbstractAccess;
import com.exonum.binding.core.util.LibraryLoader;
import com.exonum.messages.core.Blockchain.CallInBlock;
import com.exonum.messages.core.runtime.Errors.ExecutionError;
import com.google.common.annotations.VisibleForTesting;

import java.util.Optional;


/**
 * Provides access to blockchain data of a particular service instance.
 *
 * <p>The service instance data is accessible via a
 * {@link com.exonum.binding.core.storage.database.Prefixed} access which isolates the service data
 * from all the other instances.
 *
 * <p>On top of that, this class provides read-only access to persistent data of:
 * <ul>
 *   <li>Exonum Core</li>
 *   <li>Dispatcher Service</li>
 *   <li>Other services.</li>
 * </ul>
 *
 * <p>As most native proxies, BlockchainData works in a {@linkplain Cleaner scope} that is usually
 * managed by the framework. When that scope is closed, the BlockchainData becomes inaccessible.
 * All accesses and indexes created with them also become inaccessible.
 */
public final class CallRecords extends AbstractNativeProxy {

  private static final Serializer<ExecutionError> EXECUTION_ERROR_SERIALIZER =
      protobuf(ExecutionError.class);

  static {
    LibraryLoader.load();
  }

  private final Cleaner cleaner;

  private CallRecords(NativeHandle handle, Cleaner cleaner) {
    super(handle);
    this.cleaner = cleaner;
  }

  /**
   * Creates a new BlockchainData from the native handle. The destructor will be registered
   * in the given cleaner.
   *
   * @param bdNativeHandle a handle of the native BlockchainData object
   * @param cleaner a cleaner to destroy the native peer and any dependent objects
   */
  public static CallRecords fromHandle(long bdNativeHandle, Cleaner cleaner) {
    checkNotNull(cleaner);
    return fromHandleInternal(bdNativeHandle, cleaner);
  }

  /**
   * Creates a BlockchainData for the service with the given name.
   *
   * @param baseAccess the base database access, must be a "RawAccess"
   * @param blockHeight a height of the block
   */
  @VisibleForTesting
  public static CallRecords fromRawAccess(AbstractAccess baseAccess, long blockHeight) {
    Cleaner cleaner = baseAccess.getCleaner();
    long nativeHandle = nativeCreate(baseAccess.getAccessNativeHandle(), blockHeight);
    return fromHandleInternal(nativeHandle, cleaner);
  }

  private static native long nativeCreate(long baseAccessNativeHandle, long blockHeight);

  /**
   * Expects validated parameters so that it does not throw and registers the destructor
   * properly, which is *required* to prevent leaks.
   */
  private static CallRecords fromHandleInternal(long nativeHandle, Cleaner cleaner) {
    NativeHandle handle = new NativeHandle(nativeHandle);
    ProxyDestructor.newRegistered(cleaner, handle, BlockchainData.class,
        CallRecords::nativeFree);
    return new CallRecords(handle, cleaner);
  }

  private static native void nativeFree(long bdNativeHandle);

  /**
   * TODO.
   * @param callInBlock todo
   * @return Todo
   */
  public Optional<ExecutionError> get(CallInBlock callInBlock) {
    byte[] serializedExecutionError = nativeGet(getNativeHandle(), callInBlock.toByteArray());
    if (serializedExecutionError == null) {
      return Optional.empty();
    } else {
      return Optional.of(EXECUTION_ERROR_SERIALIZER.fromBytes(serializedExecutionError));
    }
  }

  private static native byte[] nativeGet(long nativeHandle, byte[] callInBlock);
}
