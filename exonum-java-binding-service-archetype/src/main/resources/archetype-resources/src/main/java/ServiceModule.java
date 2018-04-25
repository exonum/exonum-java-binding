package ${groupId};

import com.exonum.binding.service.Service;
import com.exonum.binding.service.TransactionConverter;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * A service module defines bindings required to create an instance of {@link $.MyService}.
 */
public final class ServiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Service.class).to(MyService.class).in(Singleton.class);
    bind(TransactionConverter.class).to(MyTransactionConverter.class);
  }
}
