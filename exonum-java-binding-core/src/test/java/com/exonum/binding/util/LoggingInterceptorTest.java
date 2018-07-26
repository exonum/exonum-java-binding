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
import com.exonum.binding.service.adapters.ViewFactory;
import com.exonum.binding.service.adapters.ViewProxyFactory;
import com.exonum.binding.transport.Server;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LoggingInterceptorTest {

  private static final String EXCEPTION_MESSAGE = "Some exception";

  private ListAppender appender;

  private UserServiceAdapter serviceAdapter;

  @Before
  public void setUp() {
    final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    final Configuration config = ctx.getConfiguration();
    appender = (ListAppender) config.getAppenders().get("ListAppender");

    Injector injector = Guice.createInjector(new TestModule());
    serviceAdapter = injector.getInstance(UserServiceAdapter.class);
  }

  @After
  public void tearDown() {
    appender.clear();
  }

  @Test
  public void logInterceptedException() {
    try {
      serviceAdapter.getId();
      fail("getId() must throw");
    } catch (Throwable throwable) {
      assertThat(throwable, instanceOf(OutOfMemoryError.class));
      assertThat(throwable.getMessage(), equalTo(EXCEPTION_MESSAGE));
      assertThat(appender.getMessages().size(), equalTo(1));
    }
  }

  static class TestModule extends AbstractModule {

    @Override
    protected void configure() {
      Service service = mock(Service.class);
      when(service.getId()).thenThrow(new OutOfMemoryError(EXCEPTION_MESSAGE));
      bind(Service.class).toInstance(service);
      bind(Server.class).toInstance(mock(Server.class));
      bind(ViewFactory.class).toInstance(ViewProxyFactory.getInstance());

      bindInterceptor(subclassesOf(UserServiceAdapter.class), any(), new LoggingInterceptor());

      bind(UserServiceAdapter.class);
    }
  }
}
