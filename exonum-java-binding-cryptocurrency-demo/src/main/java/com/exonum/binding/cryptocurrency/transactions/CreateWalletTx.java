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

package com.exonum.binding.cryptocurrency.transactions;

import static com.exonum.binding.common.crypto.CryptoFunctions.Ed25519.PUBLIC_KEY_BYTES;
import static com.exonum.binding.common.crypto.CryptoUtils.byteArrayToHex;
import static com.exonum.binding.cryptocurrency.CryptocurrencyService.PROTOCOL_VERSION;
import static com.exonum.binding.cryptocurrency.CryptocurrencyServiceImpl.CRYPTO_FUNCTION;
import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkTransaction;
import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.message.BinaryMessage;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.transaction.AbstractTransaction;
import com.exonum.binding.transaction.Transaction;
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

    checkArgument(ownerPublicKey.size() == PUBLIC_KEY_BYTES,
        "Public key has invalid size (%s), must be %s bytes long.", ownerPublicKey.size(),
        PUBLIC_KEY_BYTES);
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
      TxMessageProtos.CreateWalletTx messageBody =
          TxMessageProtos.CreateWalletTx.parseFrom(message.getBody());

      PublicKey ownerPublicKey = PublicKey.fromBytes(
          (messageBody.getOwnerPublicKey().toByteArray()));
      long initialBalance = messageBody.getInitialBalance();
      return new CreateWalletTx(message, ownerPublicKey, initialBalance);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(
          "Unable to instantiate TxMessageProtos.CreateWalletTx instance from provided"
              + " binary data", e);
    }
  }

  @Override
  public boolean isValid() {
    return getMessage().verify(CRYPTO_FUNCTION, ownerPublicKey);
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
    TransactionJsonMessage<CreateWalletTx> msg = TransactionJsonMessage.<CreateWalletTx>builder()
        .protocolVersion(PROTOCOL_VERSION)
        .serviceId(CryptocurrencyService.ID)
        .messageId(CreateWalletTx.ID)
        .body(this)
        .signature(byteArrayToHex(getMessage().getSignature()))
        .build();

    return CryptocurrencyTransactionGson.instance().toJson(msg);
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
