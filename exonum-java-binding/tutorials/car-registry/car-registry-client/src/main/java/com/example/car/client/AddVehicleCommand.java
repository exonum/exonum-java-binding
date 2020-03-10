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

package com.example.car.client;

import com.example.car.messages.Transactions;
import com.example.car.messages.VehicleOuterClass.Vehicle;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.message.TransactionMessage;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "add-vehicle",
    aliases = {"av"},
    description = {"Adds a new vehicle to the registry"})
public final class AddVehicleCommand implements Callable<Integer> {

  private static final int ADD_VEHICLE_TX_ID = 0;

  @Parameters(index = "0")
  String id;

  @Parameters(index = "1")
  String make;

  @Parameters(index = "2")
  String model;

  @Parameters(index = "3")
  String owner;

  @Override
  protected TransactionMessage createTxMessage(int serviceId, KeyPair keyPair) {
    return TransactionMessage.builder()
        .serviceId(serviceId)
        .transactionId(ADD_VEHICLE_TX_ID)
        .payload(
            Transactions.AddVehicle.newBuilder()
                .setNewVehicle(
                    Vehicle.newBuilder()
                        .setId(id)
                        .setMake(make)
                        .setModel(model)
                        .setOwner(owner))
                .build())
        .sign(keyPair);
  }
}
