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
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.message.TransactionMessage;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "change-owner",
    aliases = {"co"},
    description = {"Changes the owner of the vehicle with the given ID."},
    mixinStandardHelpOptions = true)
public class ChangeOwnerCommand extends AbstractSubmitTxCommand implements Callable<Integer> {

  private static final int CHANGE_OWNER_TX_ID = 1;

  @Parameters(index = "0")
  String vehicleId;

  @Parameters(index = "1")
  String newOwner;

  @Override
  protected TransactionMessage createTxMessage(int serviceId, KeyPair keyPair) {
    return TransactionMessage.builder()
        .serviceId(serviceId)
        .transactionId(CHANGE_OWNER_TX_ID)
        .payload(
            Transactions.ChangeOwner.newBuilder()
                .setId(vehicleId)
                .setNewOwner(newOwner)
                .build())
        .sign(keyPair);
  }
}
