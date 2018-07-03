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

package com.exonum.binding.cryptocurrency.transactions;

import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkTransaction;
import static com.google.common.base.Preconditions.checkArgument;
import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_PUBLICKEYBYTES;

import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.messages.AbstractTransaction;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.MapIndex;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;

/** A transaction that creates a new named wallet with default balance. */
public final class CreateWalletTx extends AbstractTransaction implements Transaction {

  static final short ID = 1;

  private final PublicKey ownerPublicKey;
  private final long initialBalance;

  @VisibleForTesting
  CreateWalletTx(BinaryMessage message, PublicKey ownerPublicKey, long initialBalance) {
    super(message);

    checkArgument(ownerPublicKey.size() == CRYPTO_SIGN_ED25519_PUBLICKEYBYTES,
        "Public key has invalid size (%s), must be %s bytes long.", ownerPublicKey.size(),
        CRYPTO_SIGN_ED25519_PUBLICKEYBYTES);
    checkArgument(initialBalance >= 0, "The initial balance (%s) must not be negative.",
        initialBalance);

    this.ownerPublicKey = ownerPublicKey;
    this.initialBalance = initialBalance;
  }

  /**
   * Creates a create wallet transaction from its message.
   * @param message a transaction message
   */
  public static CreateWalletTx fromMessage(BinaryMessage message) {
    checkTransaction(message, ID);

    try {
      TxMessagesProtos.CreateWalletTx messageBody =
          TxMessagesProtos.CreateWalletTx.parseFrom(message.getBody());

      PublicKey ownerPublicKey = PublicKey.fromBytes(
          (messageBody.getOwnerPublicKey().toByteArray()));
      long initialBalance = messageBody.getInitialBalance();
      return new CreateWalletTx(message, ownerPublicKey, initialBalance);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(
          "Unable to instantiate TxMessagesProtos.CreateWalletTx instance from provided"
              + " binary data", e);
    }
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public void execute(Fork view) {
    CryptocurrencySchema schema = new CryptocurrencySchema(view);
    MapIndex<PublicKey, Wallet> wallets = schema.wallets();

    if (wallets.containsKey(ownerPublicKey)) {
      return;
    }

    Wallet wallet = new Wallet(initialBalance);

    wallets.put(ownerPublicKey, wallet);
  }

  @Override
  public String info() {
    return CryptocurrencyTransactionGson.instance().toJson(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateWalletTx that = (CreateWalletTx) o;
    return Objects.equals(ownerPublicKey, that.ownerPublicKey)
        && initialBalance == that.initialBalance;
  }

  @Override
  public int hashCode() {
    return Objects.hash(ownerPublicKey, initialBalance);
  }
}
