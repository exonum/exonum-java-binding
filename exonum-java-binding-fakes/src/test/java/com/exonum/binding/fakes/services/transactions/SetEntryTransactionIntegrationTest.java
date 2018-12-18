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

package com.exonum.binding.fakes.services.transactions;

import static com.exonum.binding.fakes.services.transactions.SetEntryTransaction.ENTRY_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.indices.EntryIndexProxy;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.transaction.TransactionContext;
import com.exonum.binding.util.LibraryLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@RequiresNativeLibrary
class SetEntryTransactionIntegrationTest {

  @BeforeAll
  static void loadLibrary() {
    LibraryLoader.load();
  }

  @Test
  void executePutsTheValueIntoEntry() throws CloseFailuresException {
    try (MemoryDb database = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      String value = "A value to set into entry";
      Fork fork = database.createFork(cleaner);

      SetEntryTransaction tx = new SetEntryTransaction(value);
      TransactionContext context = spy(TransactionContext.builder()
          .fork(fork)
          .hash(HashCode.fromInt(123))
          .authorPk(PublicKey.fromHexString("1234"))
          .build());

      tx.execute(context);

      database.merge(fork);

      Snapshot snapshot = database.createSnapshot(cleaner);
      EntryIndexProxy entry = EntryIndexProxy.newInstance(ENTRY_NAME, snapshot,
          StandardSerializers.string());
      assertTrue(entry.isPresent());
      assertThat(entry.get(), equalTo(value));

      //verify context
      verify(context).getFork();
      verify(context).getTransactionMessageHash();
      verify(context).getAuthorPk();
    }
  }
}
