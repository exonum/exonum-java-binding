package com.exonum.binding.service1;

import com.exonum.binding.messages.AbstractTransaction;
import com.exonum.binding.service.AbstractService;
import com.exonum.binding.service.Node;
import com.exonum.binding.service.Schema;
import com.exonum.binding.service.Service;
import com.exonum.binding.service.TransactionConverter;
import com.exonum.binding.service.spi.ServiceFactory;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.View;
import io.vertx.ext.web.Router;

public class MyServiceFactory implements ServiceFactory {

  private static final TransactionConverter TRANSACTION_CONVERTER = (message) -> new AbstractTransaction(message) {

    @Override
    public boolean isValid() {
      return false;
    }

    @Override
    public void execute(Fork view) {

    }
  };

  @Override
  public Service createService() {
    return new AbstractService((short) 0x01, "my-service", TRANSACTION_CONVERTER) {
      @Override
      protected Schema createDataSchema(View view) {
        return new Schema() {};
      }

      @Override
      public void createPublicApiHandlers(Node node, Router router) {}
    };
  }
}
