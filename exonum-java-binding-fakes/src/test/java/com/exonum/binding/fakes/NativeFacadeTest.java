package com.exonum.binding.fakes;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.fakes.services.service.TestService;
import com.exonum.binding.service.adapters.UserServiceAdapter;
import com.exonum.binding.service.adapters.UserTransactionAdapter;
import org.junit.Test;

public class NativeFacadeTest {

  private static final String TX_VALUE = "value";
  private static final String TX_INFO = "{}";

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
