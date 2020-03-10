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

import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.client.ExonumClient;
import java.util.concurrent.Callable;
import picocli.CommandLine.ArgGroup;

/**
 * An abstract command submitting a transaction to Exonum node.
 */
abstract class AbstractSubmitTxCommand implements Callable<Integer> {

  @ArgGroup(exclusive = true, multiplicity = "1")
  ServiceIds serviceIds;

  @Override
  public Integer call() {
    var client = ExonumClient.newBuilder()
        .setExonumHost(Config.NODE_PUBLIC_API_HOST)
        .build();
    var serviceId = findServiceId(client);
    // todo: Shall we add a command to generate a keypair and let all
    //   other use that keypair, so that the user learns at least that each transaction comes
    //   signed with a key into the network?
    var keyPair = CryptoFunctions.ed25519().generateKeyPair();
    var txMessage = createTxMessage(serviceId, keyPair);
    client.submitTransaction(txMessage);
    return 0;
  }

  protected abstract TransactionMessage createTxMessage(int serviceId, KeyPair keyPair);

  private int findServiceId(ExonumClient client) {
    var serviceIdResolver = new ServiceIdResolver(serviceIds, client);
    return serviceIdResolver.getId();
  }
}
