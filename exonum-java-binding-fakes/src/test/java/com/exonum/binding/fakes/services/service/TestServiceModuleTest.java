package com.exonum.binding.fakes.services.service;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import com.exonum.binding.service.Service;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Test;

public class TestServiceModuleTest {

  @Test
  public void configure() {
    Injector injector = Guice.createInjector(new TestServiceModule());

    Service instance = injector.getInstance(Service.class);

    assertNotNull(instance);
    assertThat(instance, instanceOf(TestService.class));
  }
}
