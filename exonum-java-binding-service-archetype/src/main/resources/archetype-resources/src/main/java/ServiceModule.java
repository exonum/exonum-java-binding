package ${groupId};

import com.exonum.binding.service.Service;
import com.exonum.binding.service.TransactionConverter;
import com.google.inject.AbstractModule;

/**
 * A service module defines bindings required to create an instance of {@link $.MyService}.
 */
public class ServiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Service.class).to(MyService.class);
    bind(TransactionConverter.class).to(MyTransactionConverter.class);
  }
}
