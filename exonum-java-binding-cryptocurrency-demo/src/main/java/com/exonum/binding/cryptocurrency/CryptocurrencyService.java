/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.cryptocurrency;

import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.hash.HashCode;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.service.Service;
import java.util.Optional;

public interface CryptocurrencyService extends Service {
  short ID = 42;
  String NAME = "cryptocurrency-demo-service";

  HashCode submitTransaction(Transaction tx);

  Optional<Wallet> getWallet(PublicKey ownerKey);
}
