package com.exonum.binding.cryptocurrency;

import com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionConverter;
import com.exonum.binding.service.Service;
import com.exonum.binding.service.TransactionConverter;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class ServiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Service.class).to(CryptocurrencyServiceImpl.class);
    bind(CryptocurrencyService.class).to(CryptocurrencyServiceImpl.class).in(Singleton.class);
    bind(TransactionConverter.class).to(CryptocurrencyTransactionConverter.class);
  }
}
