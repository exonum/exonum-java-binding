package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.transaction.RawTransaction;
import com.google.protobuf.ByteString;

public class CreateMultisignTransferTransactionUtils {

  static RawTransaction createRawTransaction(long seed, PublicKey recipientId, long amount) {
    return RawTransaction.newBuilder()
        .serviceId(CryptocurrencyService.ID)
        .transactionId(MultisignTransferTx.ID)
        .payload(TxMessageProtos.TransferTx.newBuilder()
            .setSeed(seed)
            .setToWallet(fromPublicKey(recipientId))
            .setSum(amount)
            .build()
            .toByteArray())
        .build();
  }

  /**
   * Returns key byte string.
   */
  private static ByteString fromPublicKey(PublicKey k) {
    return ByteString.copyFrom(k.toBytes());
  }

  /**
   * Creates wallets with initial balance and adds them to database view.
   */
  static void createWallet(Fork view, PublicKey publicKey,
                           Long initialBalance,
                           Long pendingInitialBalance,
                           PublicKey signer) {
    CryptocurrencySchema schema = new CryptocurrencySchema(view);
    MapIndex<PublicKey, Wallet> wallets = schema.wallets();
    wallets.put(publicKey, new Wallet(initialBalance, pendingInitialBalance, signer));
  }

  private CreateMultisignTransferTransactionUtils() {
    throw new AssertionError("Non-instantiable");
  }
}
