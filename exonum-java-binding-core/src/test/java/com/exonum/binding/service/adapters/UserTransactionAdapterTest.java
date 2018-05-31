package com.exonum.binding.service.adapters;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.exonum.binding.messages.Transaction;
import com.exonum.binding.proxy.Cleaner;
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

  @Mock
  private ViewFactory viewFactory;

  @InjectMocks
  private UserTransactionAdapter transactionAdapter;

  @Test
  public void execute_closesCleanerAfterExecution() throws Exception {
    long forkHandle = 0x0B;
    transactionAdapter.execute(forkHandle);

    ArgumentCaptor<Cleaner> ac = ArgumentCaptor.forClass(Cleaner.class);
    verify(viewFactory).createFork(eq(forkHandle), ac.capture());

    Cleaner cleaner = ac.getValue();
    assertTrue(cleaner.isClosed());
  }
}
