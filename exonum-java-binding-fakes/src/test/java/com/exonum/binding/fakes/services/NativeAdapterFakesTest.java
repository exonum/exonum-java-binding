package com.exonum.binding.fakes.services;

import static com.exonum.binding.fakes.services.NativeAdapterFakes.createTransactionMock;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.service.adapters.UserTransactionAdapter;
import org.junit.Test;

public class NativeAdapterFakesTest {

  @Test
  public void createValidTransaction() {
    UserTransactionAdapter tx = createTransactionMock(true);
    assertTrue(tx.isValid());
  }

  @Test
  public void createInvalidTransaction() {
    UserTransactionAdapter tx = createTransactionMock(false);
    assertFalse(tx.isValid());
  }

  @Test
  public void executeIsNoOp() {
    UserTransactionAdapter tx = createTransactionMock(true);
    long forkHandle = 10L;
    tx.execute(forkHandle);
  }

  @Test
  public void withDefaultInfo() {
    UserTransactionAdapter tx = createTransactionMock(true);
    assertThat(tx.info(), emptyString());
  }

  @Test
  public void withInfo() {
    String txInfo = "An awesome tx";
    UserTransactionAdapter tx = createTransactionMock(true, txInfo);
    assertThat(tx.info(), equalTo(txInfo));
  }

  @Test
  public void mockBasedUserTransactionAdapter() {
    UserTransactionAdapter tx = mock(UserTransactionAdapter.class);
    when(tx.isValid()).thenReturn(true);

    assertTrue(tx.isValid());
  }
}
