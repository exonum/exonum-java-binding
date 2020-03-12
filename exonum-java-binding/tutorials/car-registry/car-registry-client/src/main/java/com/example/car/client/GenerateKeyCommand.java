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

import com.exonum.binding.common.crypto.AbstractKey;
import com.exonum.binding.common.crypto.CryptoFunctions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

@Command(name = "keygen",
    aliases = {"kg"},
    description = {
        "Generates an Ed25519 key pair to sign transaction messages.",
        "Private key is written to 'exonum_id' file; public - to 'exonum_id.pub'."
    })
public final class GenerateKeyCommand implements Callable<Integer> {

  public static final String EXONUM_ID_FILENAME = "exonum_id";
  public static final String EXONUM_ID_PUB_FILENAME = "exonum_id.pub";

  @Override
  public Integer call() throws IOException {
    var keyPair = CryptoFunctions.ed25519().generateKeyPair();
    writeKeyTo(keyPair.getPrivateKey(), EXONUM_ID_FILENAME);
    writeKeyTo(keyPair.getPublicKey(), EXONUM_ID_PUB_FILENAME);
    return 0;
  }

  private static void writeKeyTo(AbstractKey key, String filename) throws IOException {
    Files.writeString(Path.of(filename), key.toString());
  }
}
