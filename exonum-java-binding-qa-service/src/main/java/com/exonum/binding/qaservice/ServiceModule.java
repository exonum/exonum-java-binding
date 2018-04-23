package com.exonum.binding.qaservice;

import com.exonum.binding.qaservice.transactions.QaTransactionConverter;
import com.exonum.binding.service.Service;
import com.exonum.binding.service.TransactionConverter;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * A module of the QA service.
 */
public final class ServiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Service.class).to(QaServiceImpl.class);
    bind(QaService.class).to(QaServiceImpl.class);
    // Make sure QaService remains a singleton.
    bind(QaServiceImpl.class).in(Singleton.class);

    bind(TransactionConverter.class).to(QaTransactionConverter.class);
  }
}
