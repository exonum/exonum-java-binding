package com.exonum.binding.util;

import static com.google.inject.matcher.Matchers.any;
import static com.google.inject.matcher.Matchers.subclassesOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.service.Service;
import com.exonum.binding.service.adapters.UserServiceAdapter;
import com.exonum.binding.transport.Server;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LoggingInterceptorTest {

  private static String EXCEPTION_MESSAGE = "Some exception";

  private TestAppender appender;

  private UserServiceAdapter serviceAdapter;

  @Before
  public void setUp() {
    final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    final Configuration config = ctx.getConfiguration();
    appender = (TestAppender) config.getAppenders().get("TestAppender");

    Injector injector = Guice.createInjector(new TestModule());
    serviceAdapter = injector.getInstance(UserServiceAdapter.class);
  }

  @Test
  public void logInterceptedException() {
    try {
      serviceAdapter.getId();
      fail("getId() must throw");
    } catch (Throwable throwable) {
      assertThat(throwable, instanceOf(OutOfMemoryError.class));
      assertThat(throwable.getMessage(), equalTo(EXCEPTION_MESSAGE));
      Assert.assertEquals(appender.getMessages().size(), 1);
    }
  }

  class TestModule extends AbstractModule {

    @Override
    protected void configure() {
      Service service = mock(Service.class);
      when(service.getId()).thenThrow(new OutOfMemoryError(EXCEPTION_MESSAGE));
      bind(Service.class).toInstance(service);
      bind(Server.class).toInstance(mock(Server.class));

      bindInterceptor(subclassesOf(UserServiceAdapter.class), any(), new LoggingInterceptor());

      bind(UserServiceAdapter.class);
    }
  }
}
