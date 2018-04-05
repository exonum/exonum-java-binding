package ${groupId};

import com.exonum.binding.service.Service;
import com.exonum.binding.service.TransactionConverter;
import com.google.inject.AbstractModule;

public class ServiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Service.class).to(MyService.class);
    bind(TransactionConverter.class).to(MyTransactionConverter.class);
  }
}
