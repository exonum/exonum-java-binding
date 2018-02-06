package com.exonum.binding.service.adapters;

import static org.mockito.Mockito.doNothing;

import com.exonum.binding.messages.Transaction;
import com.exonum.binding.storage.database.Fork;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UserTransactionAdapterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private Transaction transaction;

  @InjectMocks
  private UserTransactionAdapter transactionAdapter;

  @Test
  public void execute_closesForkAfterExecution() throws Exception {
    ArgumentCaptor<Fork> ac = ArgumentCaptor.forClass(Fork.class);
    doNothing().when(transaction).execute(ac.capture());

    long forkHandle = 0x0B;
    transactionAdapter.execute(forkHandle);

    Fork fork = ac.getValue();
    expectedException.expect(IllegalStateException.class);
    fork.getViewNativeHandle();
  }
}
