package com.exonum.binding.fakes.services.service;

import com.exonum.binding.service.Service;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

/**
 * A module configuring {@link TestService}.
 */
public final class TestServiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Service.class).to(TestService.class);
    bind(new TypeLiteral<SchemaFactory<TestSchema>>(){})
        .toInstance(TestSchema::new);
  }
}
