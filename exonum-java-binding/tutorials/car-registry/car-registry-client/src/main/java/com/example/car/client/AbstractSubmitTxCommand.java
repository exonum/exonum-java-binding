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

import static java.nio.file.Files.readString;

import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PrivateKey;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.client.ExonumClient;
import com.exonum.client.response.TransactionResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

/**
 * An abstract command submitting a transaction to Exonum node.
 */
abstract class AbstractSubmitTxCommand implements Callable<Integer> {

  private static final int LONG_AWAIT_TIMEOUT_MILLIS = 2000;
  private static final long SHORT_AWAIT_TIMEOUT_MILLIS = 250;
  /** The code under flag won't work till ECR-4317 is resolved. */
  private static final boolean USE_BROKEN_AWAIT = false;

  @Option(names = {"-a", "--await"},
      description = {
          "Await the transaction commit.",
          "The transaction execution result will be printed on the console."
      })
  boolean await = false;

  @ArgGroup(exclusive = true, multiplicity = "1")
  ServiceIds serviceIds;

  @Override
  public Integer call() throws Exception {
    var client = ExonumClient.newBuilder()
        .setExonumHost(Config.NODE_PUBLIC_API_HOST)
        .build();
    var serviceId = findServiceId(client);
    var keyPair = readKeyPair();
    var txMessage = createTxMessage(serviceId, keyPair);
    var txHash = client.submitTransaction(txMessage);
    awaitTxCommit(client, txHash);
    return 0;
  }

  private void awaitTxCommit(ExonumClient client, HashCode txHash) throws InterruptedException {
    if (!await) {
      return;
    }

    // If requested: await its commitment
    Optional<TransactionResponse> txInfoOpt;
    if (USE_BROKEN_AWAIT) {
      while (true) {
        Thread.sleep(SHORT_AWAIT_TIMEOUT_MILLIS);
        txInfoOpt = client.getTransaction(txHash);
        var txInfo = txInfoOpt
            .orElseThrow(() -> new IllegalStateException(
                "Explorer is broken: https://jira.bf.local/browse/ECR-4317"));
        if (txInfo.isCommitted()) {
          break;
        }
      }
    } else {
      Thread.sleep(LONG_AWAIT_TIMEOUT_MILLIS);
      txInfoOpt = client.getTransaction(txHash);
    }
    // Print the execution result
    txInfoOpt.ifPresent(System.out::println);
  }

  private int findServiceId(ExonumClient client) {
    var serviceIdResolver = new ServiceIdResolver(serviceIds, client);
    return serviceIdResolver.getId();
  }

  private static KeyPair readKeyPair() throws IOException {
    var privateKeyStr = readString(Path.of(GenerateKeyCommand.EXONUM_ID_FILENAME));
    var privateKey = PrivateKey.fromHexString(privateKeyStr);

    var pubKeyStr = readString(Path.of(GenerateKeyCommand.EXONUM_ID_PUB_FILENAME));
    var pubKey = PublicKey.fromHexString(pubKeyStr);

    return KeyPair.newInstance(privateKey, pubKey);
  }

  protected abstract TransactionMessage createTxMessage(int serviceId, KeyPair keyPair);
}
