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
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.client.ExonumClient;
import java.util.concurrent.Callable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "add-vehicle",
    aliases = {"av"},
    description = {"Adds a new vehicle to the registry"})
public final class AddVehicleCommand implements Callable<Integer> {

  private static final int ADD_VEHICLE_TX_ID = 0;

  @ArgGroup(exclusive = true, multiplicity = "1")
  ServiceIds serviceIds;

  @Parameters(index = "0")
  String id;

  @Parameters(index = "1")
  String make;

  @Parameters(index = "2")
  String model;

  @Parameters(index = "3")
  String owner;

  @Override
  public Integer call() throws Exception {
    var client = ExonumClient.newBuilder()
        .setExonumHost(Config.NODE_PUBLIC_API_HOST)
        .build();

    // todo: Shall we add a command to generate a keypair and let all other use that keypair,
    //   so that the user learns at least that each transaction comes signed with a key into
    //   the network?
    var keyPair = CryptoFunctions.ed25519().generateKeyPair();
    var txMessage = TransactionMessage.builder()
        .serviceId(findServiceId(client))
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

    // todo: Add logging
    var txHashCode = client.submitTransaction(txMessage);
    return 0;
  }

  private int findServiceId(ExonumClient client) {
    var serviceIdResolver = new ServiceIdResolver(serviceIds, client);
    return serviceIdResolver.getId();
  }
}
