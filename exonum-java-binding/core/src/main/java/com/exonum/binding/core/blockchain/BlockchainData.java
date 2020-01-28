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

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.core.proxy.AbstractNativeProxy;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.proxy.ProxyDestructor;
import com.exonum.binding.core.runtime.DispatcherSchema;
import com.exonum.binding.core.storage.database.AbstractAccess;
import com.exonum.binding.core.storage.database.Prefixed;
import com.exonum.binding.core.storage.database.ReadonlyFork;
import com.exonum.binding.core.util.LibraryLoader;
import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import javax.annotation.Nullable;

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
public final class BlockchainData extends AbstractNativeProxy {

  static {
    LibraryLoader.load();
  }

  private final Cleaner cleaner;
  @Nullable private Prefixed executingServiceAccess;

  private BlockchainData(NativeHandle handle, Cleaner cleaner) {
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
  public static BlockchainData fromHandle(long bdNativeHandle, Cleaner cleaner) {
    checkNotNull(cleaner);
    return fromHandleInternal(bdNativeHandle, cleaner);
  }

  /**
   * Creates a BlockchainData for the service with the given name.
   *
   * @param baseAccess the base database access, must be a "RawAccess"
   * @param instanceName a service instance name
   */
  @VisibleForTesting
  public static BlockchainData fromRawAccess(AbstractAccess baseAccess, String instanceName) {
    Cleaner cleaner = baseAccess.getCleaner();
    long bdNativeHandle = nativeCreate(baseAccess.getAccessNativeHandle(), instanceName);
    return fromHandleInternal(bdNativeHandle, cleaner);
  }

  private static native long nativeCreate(long baseAccessNativeHandle, String instanceName);

  /**
   * Expects validated parameters so that it does not throw and registers the destructor
   * properly, which is *required* to prevent leaks.
   */
  private static BlockchainData fromHandleInternal(long bdNativeHandle, Cleaner cleaner) {
    NativeHandle handle = new NativeHandle(bdNativeHandle);
    ProxyDestructor.newRegistered(cleaner, handle, BlockchainData.class,
        BlockchainData::nativeFree);
    return new BlockchainData(handle, cleaner);
  }

  private static native void nativeFree(long bdNativeHandle);

  /**
   * Returns the database access for the data of the current executing service.
   *
   * <p>The returned Access is writeable in contexts that allow database modifications.
   *
   * <p>Only service data is accessible through the returned access. All indexes, initialized
   * through this access, are created in a namespace, separate from other services.
   */
  public Prefixed getExecutingServiceData() {
    // Since the base access (Fork) is unknown in the main use-case (BlockchainData
    // received from  core), we must create the Prefixed access at most once so that index
    // pooling works for read-write-based Accesses.
    if (executingServiceAccess == null) {
      long nativeHandle = getNativeHandle();
      long prefixedHandle = nativeGetExecutingServiceAccess(nativeHandle);
      boolean canModify = nativeCanModify(nativeHandle);
      executingServiceAccess = Prefixed.fromHandle(prefixedHandle, canModify, cleaner);
    }
    return executingServiceAccess;
  }

  private static native long nativeGetExecutingServiceAccess(long bdNativeHandle);

  /** Returns true if the base Access is modifiable. */
  private static native boolean nativeCanModify(long bdNativeHandle);

  /**
   * Returns a <em>readonly</em> database access for the data of the service instance with
   * the given name, if it is started; or {@code Optional.empty} if no such service started.
   *
   * <p>Only service data is accessible through the returned access.
   *
   * @param instanceName the name of the service instance to which data to provide access
   */
  public Optional<Prefixed> findServiceData(String instanceName) {
    long prefixedHandle = nativeFindServiceData(getNativeHandle(), instanceName);
    if (prefixedHandle == NativeHandle.INVALID_NATIVE_HANDLE) {
      return Optional.empty();
    } else {
      Prefixed access = Prefixed.fromHandle(prefixedHandle, false, cleaner);
      return Optional.of(access);
    }
  }

  /**
   * Returns a valid handle to a Prefixed access for the data of service with the given name;
   * or 0 (nullptr) if no such service exists.
   */
  private static native long nativeFindServiceData(long bdNativeHandle, String instanceName);

  /**
   * Returns the blockchain schema (aka Exonum core schema).
   */
  public Blockchain getBlockchain() {
    return Blockchain.newInstance(getUnstructuredAccess());
  }

  /**
   * Returns the schema of the dispatcher service.
   */
  public DispatcherSchema getDispatcherSchema() {
    return new DispatcherSchema(getUnstructuredAccess());
  }

  private ReadonlyFork getUnstructuredAccess() {
    long roForkHandle = nativeGetUnstructuredAccess(getNativeHandle());
    return ReadonlyFork.fromHandle(roForkHandle, cleaner);
  }

  private static native long nativeGetUnstructuredAccess(long bdNativeHandle);
}
