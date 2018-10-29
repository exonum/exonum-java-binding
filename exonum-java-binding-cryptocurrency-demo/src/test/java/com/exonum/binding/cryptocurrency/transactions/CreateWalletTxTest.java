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

import static com.exonum.binding.common.crypto.CryptoUtils.byteArrayToHex;
import static com.exonum.binding.cryptocurrency.CryptocurrencyServiceImpl.CRYPTO_FUNCTION;
import static com.exonum.binding.cryptocurrency.transactions.CreateWalletTransactionUtils.DEFAULT_BALANCE;
import static com.exonum.binding.cryptocurrency.transactions.CreateWalletTransactionUtils.createSignedMessage;
import static com.exonum.binding.cryptocurrency.transactions.CreateWalletTransactionUtils.createUnsignedMessage;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.message.BinaryMessage;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.cryptocurrency.PredefinedOwnerKeys;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.test.Bytes;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.util.LibraryLoader;
import com.google.gson.reflect.TypeToken;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class CreateWalletTxTest {

  static {
    LibraryLoader.load();
  }

  private static final PublicKey OWNER_KEY = PredefinedOwnerKeys.FIRST_OWNER_KEY;

  @Test
  void fromMessage() {
    long initialBalance = 100L;
    BinaryMessage m = createUnsignedMessage(OWNER_KEY, initialBalance);

    CreateWalletTx tx = CreateWalletTx.fromMessage(m);

    assertThat(tx, equalTo(withMockMessage(OWNER_KEY, initialBalance)));
  }

  @Test
  void isValidSigned() {
    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair();
    BinaryMessage m = createSignedMessage(keyPair);

    CreateWalletTx tx = CreateWalletTx.fromMessage(m);

    assertTrue(tx.isValid());
  }

  @Test
  void isValidUnsigned() {
    BinaryMessage m = createUnsignedMessage(OWNER_KEY);
    CreateWalletTx tx = CreateWalletTx.fromMessage(m);

    assertFalse(tx.isValid());
  }

  @Test
  void constructorRejectsInvalidSizedKey() {
    PublicKey publicKey = PublicKey.fromBytes(new byte[1]);

    Throwable t = assertThrows(IllegalArgumentException.class,
        () -> withMockMessage(publicKey, DEFAULT_BALANCE)
    );
    assertThat(t.getMessage(), equalTo("Public key has invalid size (1), must be 32 bytes long."));
  }

  @Test
  void constructorRejectsNegativeBalance() {
    long initialBalance = -1L;

    Throwable t = assertThrows(IllegalArgumentException.class,
        () -> withMockMessage(OWNER_KEY, initialBalance)
    );
    assertThat(t.getMessage(), equalTo("The initial balance (-1) must not be negative."));

  }

  @Test
  @RequiresNativeLibrary
  void executeCreateWalletTx() throws CloseFailuresException {
    CreateWalletTx tx = withMockMessage(OWNER_KEY, DEFAULT_BALANCE);

    try (Database db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      tx.execute(view);

      // Check that entries have been added.
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      MapIndex<PublicKey, Wallet> wallets = schema.wallets();

      assertTrue(wallets.containsKey(OWNER_KEY));
      assertThat(wallets.get(OWNER_KEY).getBalance(), equalTo(DEFAULT_BALANCE));
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeAlreadyExistingWalletTx() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      Long initialBalance = DEFAULT_BALANCE;

      // Create a wallet manually.
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      {
        MapIndex<PublicKey, Wallet> wallets = schema.wallets();
        wallets.put(OWNER_KEY, new Wallet(initialBalance));
      }

      // Execute the transaction, that has the same owner key.
      // Use twice the initial balance to detect invalid updates.
      long newBalance = 2 * initialBalance;
      CreateWalletTx tx = withMockMessage(OWNER_KEY, newBalance);
      tx.execute(view);

      // Check it has not changed the entries in the maps.
      {
        MapIndex<PublicKey, Wallet> wallets = schema.wallets();
        assertTrue(wallets.containsKey(OWNER_KEY));
        assertThat(wallets.get(OWNER_KEY).getBalance(), equalTo(initialBalance));
      }
    }
  }

  @Test
  void info() {
    CreateWalletTx tx = withMockMessage(OWNER_KEY, DEFAULT_BALANCE);

    String info = tx.info();
    TransactionJsonMessage<CreateWalletTx> txParams = CryptocurrencyTransactionGson.instance()
        .fromJson(info, new TypeToken<TransactionJsonMessage<CreateWalletTx>>() {
        }.getType());

    assertThat(txParams.getServiceId(), equalTo(CryptocurrencyService.ID));
    assertThat(txParams.getMessageId(), equalTo(CreateWalletTx.ID));
    assertThat(txParams.getBody(), equalTo(tx));
    assertThat(txParams.getSignature(), equalTo(byteArrayToHex(tx.getMessage().getSignature())));
  }

  @Test
  void verifyEquals() {
    EqualsVerifier
        .forClass(CreateWalletTx.class)
        .verify();
  }

  private static CreateWalletTx withMockMessage(PublicKey ownerKey, long initialBalance) {
    BinaryMessage message = mock(BinaryMessage.class);
    lenient().when(message.getSignature()).thenReturn(Bytes.bytes(0x00, 0x01, 0x02));
    return new CreateWalletTx(message, ownerKey, initialBalance);
  }
}
