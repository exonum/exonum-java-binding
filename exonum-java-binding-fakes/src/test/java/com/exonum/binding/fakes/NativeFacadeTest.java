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

package com.exonum.binding.fakes;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.fakes.services.service.TestService;
import com.exonum.binding.service.adapters.UserServiceAdapter;
import com.exonum.binding.service.adapters.UserTransactionAdapter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NativeFacadeTest {

  private static final String TX_VALUE = "value";
  private static final String TX_INFO = "{}";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void createValidTransaction() {
    UserTransactionAdapter tx = NativeFacade.createTransaction(true, TX_VALUE, TX_INFO);

    assertTrue(tx.isValid());
  }

  @Test
  public void createInvalidTransaction() {
    UserTransactionAdapter tx = NativeFacade.createTransaction(false, TX_VALUE, TX_INFO);

    assertFalse(tx.isValid());
  }

  @Test
  public void createThrowingIllegalArgumentInInfo() {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    UserTransactionAdapter transaction = NativeFacade.createThrowingTransaction(exceptionType);

    expectedException.expect(exceptionType);
    transaction.info();
  }

  @Test
  public void createTransactionWithInfo() {
    String txInfo = "{ \"info\": \"A custom transaction information\" }";
    UserTransactionAdapter tx = NativeFacade.createTransaction(true, TX_VALUE, txInfo);

    assertThat(tx.info(), equalTo(txInfo));
  }

  @Test
  public void createTestService() {
    UserServiceAdapter service = NativeFacade.createTestService();

    assertThat(service.getId(), equalTo(TestService.ID));
  }
}
