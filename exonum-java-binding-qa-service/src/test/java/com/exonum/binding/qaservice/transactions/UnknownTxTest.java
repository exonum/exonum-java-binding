package com.exonum.binding.qaservice.transactions;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.exonum.binding.storage.database.Fork;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class UnknownTxTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void isValid() {
    UnknownTx tx = new UnknownTx();

    assertTrue(tx.isValid());
  }

  @Test
  public void execute() {
    UnknownTx tx = new UnknownTx();

    expectedException.expect(AssertionError.class);
    tx.execute(mock(Fork.class));
  }
}
