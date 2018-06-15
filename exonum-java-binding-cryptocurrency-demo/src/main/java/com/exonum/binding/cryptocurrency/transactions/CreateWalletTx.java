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

import static com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionTemplate.newCryptocurrencyTransactionBuilder;
import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkTransaction;
import static com.google.common.base.Preconditions.checkArgument;
import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_PUBLICKEYBYTES;

import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.crypto.PublicKeySerializer;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.cryptocurrency.transactions.converters.TransactionMessageConverter;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.serialization.Serializer;
import com.google.common.base.Objects;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.nio.ByteBuffer;

/** A transaction that creates a new named wallet with default balance. */
public final class CreateWalletTx extends BaseTx implements Transaction {

  private static final short ID = CryptocurrencyTransaction.CREATE_WALLET.getId();

  private static final Serializer<PublicKey> publicKeySerializer = PublicKeySerializer.INSTANCE;

  static final long DEFAULT_BALANCE = 100L;

  private final PublicKey ownerPublicKey;

  /**
   * Creates a new wallet creation transaction with given name.
   */
  public CreateWalletTx(PublicKey ownerPublicKey) {
    super(CryptocurrencyService.ID, ID);
    checkArgument(
        ownerPublicKey.size() == CRYPTO_SIGN_ED25519_PUBLICKEYBYTES,
        "Public key must have correct size");
    this.ownerPublicKey = ownerPublicKey;
  }

  static TransactionMessageConverter<CreateWalletTx> converter() {
    return TransactionConverter.INSTANCE;
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

    Wallet wallet = new Wallet(DEFAULT_BALANCE);

    wallets.put(ownerPublicKey, wallet);
  }

  @Override
  public String info() {
    return CryptocurrencyTransactionGson.instance().toJson(this);
  }

  @Override
  public BinaryMessage getMessage() {
    return converter().toMessage(this);
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
    return service_id == that.service_id
        && message_id == that.message_id
        && Objects.equal(ownerPublicKey, that.ownerPublicKey);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(service_id, message_id, ownerPublicKey);
  }

  private enum TransactionConverter implements TransactionMessageConverter<CreateWalletTx> {
    INSTANCE;

    @Override
    public CreateWalletTx fromMessage(Message txMessage) {
      checkTransaction(txMessage, ID);

      CreateWalletTx createWalletTx;
      try {
        TxMessagesProtos.CreateWalletTx messageBody =
            TxMessagesProtos.CreateWalletTx.parseFrom(txMessage.getBody());

        PublicKey ownerPublicKey =
            PublicKey.fromBytes((messageBody.getOwnerPublicKey().getRawKey().toByteArray()));
        createWalletTx = new CreateWalletTx(ownerPublicKey);
      } catch (InvalidProtocolBufferException e) {
        throw new IllegalArgumentException(
            "Unable to instantiate TxMessagesProtos.CreateWalletTx instance from provided"
                + " binary data", e);
      }
      return createWalletTx;
    }

    @Override
    public BinaryMessage toMessage(CreateWalletTx transaction) {
      PublicKeyProtos.PublicKey ownerPublicKey = PublicKeyProtos.PublicKey.newBuilder()
          .setRawKey(ByteString.copyFrom(publicKeySerializer.toBytes(transaction.ownerPublicKey)))
          .build();
      TxMessagesProtos.CreateWalletTx createWalletTx =
          TxMessagesProtos.CreateWalletTx.newBuilder()
              .setOwnerPublicKey(ownerPublicKey)
              .build();

      ByteBuffer buffer = ByteBuffer.wrap(createWalletTx.toByteArray());

      return newCryptocurrencyTransactionBuilder(ID).setBody(buffer).buildRaw();
    }
  }
}
