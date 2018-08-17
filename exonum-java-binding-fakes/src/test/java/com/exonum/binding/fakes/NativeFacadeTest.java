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

package com.exonum.binding.fakes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.fakes.services.service.TestService;
import com.exonum.binding.service.adapters.UserServiceAdapter;
import com.exonum.binding.service.adapters.UserTransactionAdapter;
import org.junit.jupiter.api.Test;

class NativeFacadeTest {

  private static final String TX_VALUE = "value";
  private static final String TX_INFO = "{}";

  @Test
  void createValidTransaction() {
    UserTransactionAdapter tx = NativeFacade.createTransaction(true, TX_VALUE, TX_INFO);

    assertTrue(tx.isValid());
  }

  @Test
  void createInvalidTransaction() {
    UserTransactionAdapter tx = NativeFacade.createTransaction(false, TX_VALUE, TX_INFO);

    assertFalse(tx.isValid());
  }

  @Test
  void createThrowingIllegalArgumentInInfo() {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    UserTransactionAdapter transaction = NativeFacade.createThrowingTransaction(exceptionType);

    assertThrows(exceptionType, transaction::info);
  }

  @Test
  void createTransactionWithInfo() {
    String txInfo = "{ \"info\": \"A custom transaction information\" }";
    UserTransactionAdapter tx = NativeFacade.createTransaction(true, TX_VALUE, txInfo);

    assertThat(tx.info(), equalTo(txInfo));
  }

  @Test
  void createTestService() {
    UserServiceAdapter service = NativeFacade.createTestService();

    assertThat(service.getId(), equalTo(TestService.ID));
  }
}
