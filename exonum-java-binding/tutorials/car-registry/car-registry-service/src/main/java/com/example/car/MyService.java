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

import com.example.car.messages.Transactions;
import com.example.car.messages.VehicleOuterClass.Vehicle;
import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.service.AbstractService;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.core.transaction.ExecutionException;
import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.core.transaction.TransactionContext;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;

public final class MyService extends AbstractService {

  public static final int ADD_VEHICLE_TX_ID = 0;
  public static final int CHANGE_OWNER_TX_ID = 1;
  public static final byte ID_ALREADY_EXISTS_ERROR_CODE = 100;
  public static final byte NO_VEHICLE_ERROR_CODE = 101;

  @Inject
  public MyService(ServiceInstanceSpec instanceSpec) {
    super(instanceSpec);
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {}

  // ci-block ci-add-vehicle {
  @Transaction(ADD_VEHICLE_TX_ID)
  public void addVehicle(Transactions.AddVehicle args, TransactionContext context) {
    var serviceData = context.getServiceData();
    var schema = new MySchema(serviceData);
    ProofMapIndexProxy<String, Vehicle> vehicles = schema.vehicles();

    // Check there is no vehicle with such id in the registry already
    var newVehicle = args.getNewVehicle();
    var id = newVehicle.getId();
    if (vehicles.containsKey(id)) {
      var existingVehicle = vehicles.get(id);
      var errorDescription = String.format("The registry already contains a vehicle "
          + "with id (%s): existing=%s, new=%s", id, existingVehicle, newVehicle);
      throw new ExecutionException(ID_ALREADY_EXISTS_ERROR_CODE, errorDescription);
    }

    // Add the vehicle to the registry
    vehicles.put(id, newVehicle);
  }
  // }

  // ci-block ci-change-owner {
  @Transaction(CHANGE_OWNER_TX_ID)
  public void changeOwner(Transactions.ChangeOwner args, TransactionContext context) {
    var serviceData = context.getServiceData();
    var schema = new MySchema(serviceData);
    ProofMapIndexProxy<String, Vehicle> vehicles = schema.vehicles();

    // Check the vehicle with such ID exists
    var id = args.getId();
    if (!vehicles.containsKey(id)) {
      throw new ExecutionException(NO_VEHICLE_ERROR_CODE, "No vehicle with such id: " + id);
    }

    // Update the owner
    // Get the current entry
    var vehicleEntry = vehicles.get(id);
    // Update the owner
    var newOwner = args.getNewOwner();
    var updatedVehicleEntry = Vehicle.newBuilder(vehicleEntry)
        .setOwner(newOwner)
        .build();
    // Write it back to the registry
    vehicles.put(id, updatedVehicleEntry);
  }
  // }
}
