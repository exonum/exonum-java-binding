package com.exonum.binding.util;

import static com.google.inject.matcher.Matchers.any;
import static com.google.inject.matcher.Matchers.subclassesOf;

import com.exonum.binding.messages.AbstractTransaction;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.service.adapters.UserTransactionAdapter;
import com.exonum.binding.storage.database.Fork;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LoggingInterceptorTest {

  private static String EXCEPTION_MESSAGE = "Some exception";

  private TestAppender appender;

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Mock
  private BinaryMessage binaryMessage;

  @Mock
  private Transaction transaction;

  @InjectMocks
  private UserTransactionAdapter transactionAdapter;

  @Before
  public void setUp() {
    final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    final Configuration config = ctx.getConfiguration();
    appender = (TestAppender) config.getAppenders().get("TestAppender");

    Injector injector = Guice.createInjector(new TestModule());
    transactionAdapter = injector.getInstance(UserTransactionAdapter.class);
  }

  @Test
  public void logInterceptedException() {
    try {
      transactionAdapter.info();
    } catch (Throwable throwable) {
      Assert.assertEquals(appender.getMessages().size(), 1);
      exception.expect(OutOfMemoryError.class);
      exception.expectMessage(EXCEPTION_MESSAGE);
      throw throwable;
    }
  }

  class TestModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(Transaction.class)
          .toInstance(new AbstractTransaction(binaryMessage) {

            @Override
            public boolean isValid() {
              return true;
            }

            @Override
            public void execute(Fork view) {
              System.out.println("Transaction#execute");
            }

            @Override
            public String info() {
              throw new OutOfMemoryError(EXCEPTION_MESSAGE);
            }
          });

      bindInterceptor(subclassesOf(UserTransactionAdapter.class), any(), new LoggingInterceptor());
    }
  }
}
