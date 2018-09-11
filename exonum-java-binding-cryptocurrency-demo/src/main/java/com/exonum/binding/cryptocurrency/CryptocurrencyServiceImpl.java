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

import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.cryptocurrency.transactions.JsonBinaryMessageConverter;
import com.exonum.binding.service.AbstractService;
import com.exonum.binding.service.InternalServerError;
import com.exonum.binding.service.InvalidTransactionException;
import com.exonum.binding.service.Node;
import com.exonum.binding.service.Schema;
import com.exonum.binding.service.TransactionConverter;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.transaction.Transaction;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import java.util.Optional;
import javax.annotation.Nullable;

/** A cryptocurrency demo service. */
public final class CryptocurrencyServiceImpl extends AbstractService
    implements CryptocurrencyService {

  /** A cryptographic function for signing transaction messages of this service. */
  public static final CryptoFunction CRYPTO_FUNCTION = CryptoFunctions.ed25519();

  @Nullable private Node node;

  @Inject
  public CryptocurrencyServiceImpl(TransactionConverter transactionConverter) {
    super(ID, NAME, transactionConverter);
  }

  @Override
  protected Schema createDataSchema(View view) {
    return new CryptocurrencySchema(view);
  }

  @Override
  public Optional<String> initialize(Fork fork) {
    return Optional.empty();
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    this.node = node;

    ApiController controller = new ApiController(this, new JsonBinaryMessageConverter());
    controller.mountApi(router);
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public HashCode submitTransaction(Transaction tx) {
    checkBlockchainInitialized();
    try {
      node.submitTransaction(tx);
      return tx.hash();
    } catch (InvalidTransactionException e) {
      throw new IllegalArgumentException(e);
    } catch (InternalServerError e) {
      throw new RuntimeException("Propagated transaction submission exception", e);
    }
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public Optional<Wallet> getWallet(PublicKey ownerKey) {
    checkBlockchainInitialized();

    return node.withSnapshot((view) -> {
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      MapIndex<PublicKey, Wallet> wallets = schema.wallets();

      return Optional.ofNullable(wallets.get(ownerKey));
    });
  }

  private void checkBlockchainInitialized() {
    checkState(node != null, "Service has not been fully initialized yet");
  }
}
