package com.exonum.binding.qaservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.service.Service;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Test;

public class ServiceModuleTest {

  @Test
  public void testServiceBindingsSufficient() {
    Injector injector = createInjector();

    Service s = injector.getInstance(Service.class);

    assertThat(s).isInstanceOf(QaServiceImpl.class);
  }

  @Test
  public void testServiceIsSingleton() {
    Injector injector = createInjector();

    Service service1 = injector.getInstance(Service.class);
    Service service2 = injector.getInstance(Service.class);

    assertThat(service1).isSameAs(service2);

    QaService qaService = injector.getInstance(QaService.class);
    assertThat(service1).isSameAs(qaService);
  }

  private Injector createInjector() {
    return Guice.createInjector(new ServiceModule());
  }
}
