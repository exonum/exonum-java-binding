package com.exonum.binding.cryptocurrency.transactions;

import static com.exonum.binding.cryptocurrency.CryptocurrencyServiceImpl.CRYPTO_FUNCTION;
import static com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionTemplate.newCryptocurrencyTransactionBuilder;

import com.exonum.binding.crypto.KeyPair;
import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.google.protobuf.ByteString;

public class CreateWalletTxBase {

  static final long DEFAULT_BALANCE = 100L;

  /**
   * Creates new signed binary create wallet message using provided key pair.
   */
  public static BinaryMessage createSignedMessage(KeyPair ownerKeyPair) {
    BinaryMessage unsignedMessage = createUnsignedMessage(ownerKeyPair.getPublicKey(),
        DEFAULT_BALANCE);
    return unsignedMessage.sign(CRYPTO_FUNCTION, ownerKeyPair.getPrivateKey());
  }

  /**
   * Creates new unsigned binary create wallet message using provided key pair and default balance.
   */
  static BinaryMessage createUnsignedMessage(PublicKey ownerKey) {
    return createUnsignedMessage(ownerKey, DEFAULT_BALANCE);
  }

  /**
   * Creates new unsigned binary create wallet message using provided key pair and provided balance.
   */
  static BinaryMessage createUnsignedMessage(PublicKey ownerKey, long initialBalance) {
    return newCryptocurrencyTransactionBuilder(CreateWalletTx.ID)
        .setBody(TxMessagesProtos.CreateWalletTx.newBuilder()
            .setOwnerPublicKey(ByteString.copyFrom(ownerKey.toBytes()))
            .setInitialBalance(initialBalance)
            .build()
            .toByteArray())
        .setSignature(new byte[Message.SIGNATURE_SIZE])
        .buildRaw();
  }
}
