/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.time;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.proxy.ProxyDestructor;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.EntryIndexProxy;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

public class TimeSchemaProxy implements TimeSchema {

  private static final Serializer<PublicKey> publicKeySerializer = StandardSerializers.publicKey();
  private static final Serializer<ZonedDateTime> zonedDateTimeSerializer = StandardSerializers.zonedDateTime();

  private final NativeHandle nativeHandle;
  private final View dbView;

  private TimeSchemaProxy(NativeHandle nativeHandle, View dbView) {
    this.nativeHandle = nativeHandle;
    this.dbView = dbView;
  }

  /**
   * Constructs a schema proxy for a given dbView.
   */
  static TimeSchemaProxy newInstance(View dbView) {
    long nativePointer = nativeCreate(dbView.getViewNativeHandle());
    NativeHandle nativeHandle = new NativeHandle(nativePointer);

    Cleaner cleaner = dbView.getCleaner();
    ProxyDestructor.newRegistered(cleaner, nativeHandle, TimeSchemaProxy.class,
        TimeSchemaProxy::nativeFree);

    return new TimeSchemaProxy(nativeHandle, dbView);
  }

  @Override
  public EntryIndexProxy<ZonedDateTime> getTime() {
    return EntryIndexProxy.newInstance(TimeIndex.TIME, dbView, zonedDateTimeSerializer);
  }

  @Override
  public ProofMapIndexProxy<PublicKey, ZonedDateTime> getValidatorsTimes() {
    return ProofMapIndexProxy.newInstance(TimeIndex.VALIDATORS_TIMES, dbView, publicKeySerializer,
        zonedDateTimeSerializer);
  }

  @Override
  public List<HashCode> getStateHashes() {
    return Collections.emptyList();
  }

  private static native long nativeCreate(long viewNativeHandle);

  private static native void nativeFree(long nativeHandle);

  /**
   * Mapping for Exonum time indexes by name.
   */
  private static final class TimeIndex {
    private static final String PREFIX = "exonum_time.";
    private static final String VALIDATORS_TIMES = PREFIX + "validators_times";
    private static final String TIME = PREFIX + "time";
  }
}
