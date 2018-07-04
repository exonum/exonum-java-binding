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

import static com.exonum.binding.cryptocurrency.transactions.CreateWalletTx.DEFAULT_BALANCE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.cryptocurrency.PredefinedOwnerKeys;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.util.LibraryLoader;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CreateWalletTxTest {

  static {
    LibraryLoader.load();
  }

  private static final PublicKey ownerKey = PredefinedOwnerKeys.firstOwnerKey;

  @Rule public final ExpectedException expectedException = ExpectedException.none();

  @Test
  public void walletIsValidWithCorrectOwnerKey() {
    CreateWalletTx tx = new CreateWalletTx(ownerKey);

    assertTrue(tx.isValid());
  }

  @Test
  public void constructorRejectsInvalidSizedKey() {
    PublicKey publicKey = PublicKey.fromBytes(new byte[1]);

    expectedException.expectMessage("Public key has invalid size (1), must be 32 bytes long.");
    expectedException.expect(IllegalArgumentException.class);
    new CreateWalletTx(publicKey);
  }

  @Test
  public void executeCreateWalletTx() throws CloseFailuresException {
    CreateWalletTx tx = new CreateWalletTx(ownerKey);

    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      tx.execute(view);

      // Check that entries have been added.
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      MapIndex<PublicKey, Wallet> wallets = schema.wallets();

      assertThat(wallets.get(ownerKey).getBalance(), equalTo(DEFAULT_BALANCE));
    }
  }

  @Test
  public void executeAlreadyExistingWalletTx() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      Long balance = DEFAULT_BALANCE;

      // Create a wallet manually.
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      {
        MapIndex<PublicKey, Wallet> wallets = schema.wallets();
        wallets.put(ownerKey, new Wallet(balance));
      }

      // Execute the transaction, that has the same owner key.
      CreateWalletTx tx = new CreateWalletTx(ownerKey);
      tx.execute(view);

      // Check it has not changed the entries in the maps.
      {
        MapIndex<PublicKey, Wallet> wallets = schema.wallets();
        assertThat(wallets.get(ownerKey).getBalance(), equalTo(balance));
      }
    }
  }

  @Test
  public void info() {
    CreateWalletTx tx = new CreateWalletTx(ownerKey);

    String info = tx.info();

    BaseTx txParams = CryptocurrencyTransactionGson.instance().fromJson(info, BaseTx.class);
    assertThat(txParams.getServiceId(), equalTo(CryptocurrencyService.ID));
    assertThat(txParams.getMessageId(), equalTo(CryptocurrencyTransaction.CREATE_WALLET.getId()));
  }

  @Test
  public void converterRoundtrip() {
    CreateWalletTx tx = new CreateWalletTx(ownerKey);
    BinaryMessage message = CreateWalletTx.converter().toMessage(tx);
    CreateWalletTx txFromMessage = CreateWalletTx.converter().fromMessage(message);

    assertThat(txFromMessage, equalTo(tx));
  }

  @Test
  public void verifyEquals() {
    EqualsVerifier
        .forClass(CreateWalletTx.class)
        .verify();
  }
}
