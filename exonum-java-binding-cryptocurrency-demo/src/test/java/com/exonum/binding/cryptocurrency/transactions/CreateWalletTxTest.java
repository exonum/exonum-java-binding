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

import static com.exonum.binding.cryptocurrency.CryptocurrencyServiceImpl.CRYPTO_FUNCTION;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.exonum.binding.crypto.KeyPair;
import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.cryptocurrency.PredefinedOwnerKeys;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.util.LibraryLoader;
import com.google.protobuf.ByteString;
import java.nio.ByteBuffer;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CreateWalletTxTest {

  static {
    LibraryLoader.load();
  }

  private static final long DEFAULT_BALANCE = 100L;

  private static final PublicKey OWNER_KEY = PredefinedOwnerKeys.firstOwnerKey;

  @Rule public final ExpectedException expectedException = ExpectedException.none();

  @Test
  public void fromMessage() {
    long initialBalance = 100L;
    BinaryMessage m = createUnsignedMessage(OWNER_KEY, initialBalance);

    CreateWalletTx tx = CreateWalletTx.fromMessage(m);

    assertThat(tx, equalTo(withMockMessage(OWNER_KEY, initialBalance)));
  }

  @Test
  public void isValidSigned() {
    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair();
    BinaryMessage m = createSignedMessage(keyPair);

    CreateWalletTx tx = CreateWalletTx.fromMessage(m);

    assertTrue(tx.isValid());
  }

  @Test
  public void isValidUnsigned() {
    BinaryMessage m = createUnsignedMessage(OWNER_KEY, DEFAULT_BALANCE);
    CreateWalletTx tx = CreateWalletTx.fromMessage(m);

    assertFalse(tx.isValid());
  }

  private BinaryMessage createSignedMessage(KeyPair ownerKeyPair) {
    BinaryMessage unsignedMessage = createUnsignedMessage(ownerKeyPair.getPublicKey(),
        DEFAULT_BALANCE);
    return unsignedMessage.sign(CRYPTO_FUNCTION, ownerKeyPair.getPrivateKey());
  }

  private BinaryMessage createUnsignedMessage(PublicKey ownerKey, long initialBalance) {
    return new Message.Builder()
        .setServiceId(CryptocurrencyService.ID)
        .setMessageType(CreateWalletTx.ID)
        .setBody(ByteBuffer.wrap(TxMessagesProtos.CreateWalletTx.newBuilder()
            .setOwnerPublicKey(ByteString.copyFrom(ownerKey.toBytes()))
            .setInitialBalance(initialBalance)
            .build()
            .toByteArray()))
        .setSignature(new byte[Message.SIGNATURE_SIZE])
        .buildRaw();
  }

  @Test
  public void constructorRejectsInvalidSizedKey() {
    PublicKey publicKey = PublicKey.fromBytes(new byte[1]);

    expectedException.expectMessage("Public key has invalid size (1), must be 32 bytes long.");
    expectedException.expect(IllegalArgumentException.class);
    withMockMessage(publicKey, DEFAULT_BALANCE);
  }

  @Test
  public void constructorRejectsNegativeBalance() {
    long initialBalance = -1L;

    expectedException.expectMessage("The initial balance (-1) must not be negative.");
    expectedException.expect(IllegalArgumentException.class);
    withMockMessage(OWNER_KEY, initialBalance);
  }

  @Test
  public void executeCreateWalletTx() throws CloseFailuresException {
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
  public void executeAlreadyExistingWalletTx() throws CloseFailuresException {
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
  public void info() {
    CreateWalletTx tx = withMockMessage(OWNER_KEY, DEFAULT_BALANCE);

    String info = tx.info();

    CreateWalletTx txParams = CryptocurrencyTransactionGson.instance()
        .fromJson(info, CreateWalletTx.class);

    assertThat(txParams, equalTo(tx));
  }

  @Test
  public void verifyEquals() {
    EqualsVerifier
        .forClass(CreateWalletTx.class)
        .verify();
  }

  private static CreateWalletTx withMockMessage(PublicKey ownerKey, long initialBalance) {
    // If a normal binary message object is ever needed, take the code from the 'fromMessage' test
    // and put it here, replacing `mock(BinaryMessage.class)`.
    return new CreateWalletTx(mock(BinaryMessage.class), ownerKey, initialBalance);
  }
}
