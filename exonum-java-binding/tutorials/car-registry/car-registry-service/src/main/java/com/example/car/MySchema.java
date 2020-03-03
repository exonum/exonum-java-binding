/*
 * Copyright 2018 The Exonum Team
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

package com.example.car;

import static com.google.common.base.Preconditions.checkNotNull;

import com.example.car.messages.VehicleOuterClass.Vehicle;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.service.Schema;
import com.exonum.binding.core.storage.database.Prefixed;
import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;

/**
 * {@code MySchema} provides access to the tables of {@link MyService},
 * given a database state {@linkplain Access access object}.
 *
 * @see <a href="https://exonum.com/doc/version/0.13-rc.2/architecture/storage/#table-types">Exonum table types.</a>
 */
public final class MySchema implements Schema {

  // ci-block ci-vehicles {
  private static final Serializer<Vehicle> VEHICLE_SERIALIZER =
      StandardSerializers.protobuf(Vehicle.class);
  // }

  private final Prefixed access;

  public MySchema(Prefixed serviceData) {
    this.access = checkNotNull(serviceData);
  }

  // ci-block ci-vehicles {
  /**
   * Provides access to the current state of the vehicles registry.
   */
  public ProofMapIndexProxy<String, Vehicle> vehicles() {
    var address = IndexAddress.valueOf("vehicles");
    var keySerializer = StandardSerializers.string();
    return access.getProofMap(address, keySerializer, VEHICLE_SERIALIZER);
  }
  // }
}
