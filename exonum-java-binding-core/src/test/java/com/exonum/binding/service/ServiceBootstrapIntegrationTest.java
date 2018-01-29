package com.exonum.binding.service;

import static com.exonum.binding.messages.TemplateMessage.TEMPLATE_MESSAGE;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.messages.AbstractTransaction;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.service.adapters.UserServiceAdapter;
import com.exonum.binding.service.adapters.UserTransactionAdapter;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.View;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import org.junit.Test;

public class ServiceBootstrapIntegrationTest {

  @Test
  public void startService() throws Exception {
    UserServiceAdapter service = ServiceBootstrap.startService(
        UserModule.class.getCanonicalName(), 0);

    assertThat(service.getId(), equalTo(UserService.ID));
    assertThat(service.getName(), equalTo(UserService.NAME));
    BinaryMessage message = new Message.Builder()
        .mergeFrom(TEMPLATE_MESSAGE)
        .setServiceId(service.getId())
        .buildRaw();
    byte[] messageBytes = message.getMessage().array();

    UserTransactionAdapter transactionAdapter = service.convertTransaction(messageBytes);
    assertTrue(transactionAdapter.isValid());
  }
}

class UserModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Service.class)
        .to(UserService.class);

    bind(TransactionConverter.class)
        .toInstance((m) -> new AbstractTransaction(m) {

          @Override
          public boolean isValid() {
            return true;
          }

          @Override
          public void execute(Fork view) {
            System.out.println("Transaction#execute");
          }
        });
  }
}

class UserService extends AbstractService {

  static final short ID = 1;
  static final String NAME = "UserService";

  @Inject
  UserService(TransactionConverter transactionConverter) {
    super(ID, NAME, transactionConverter);
  }

  @Override
  protected Schema createDataSchema(View view) {
    return new Schema() {};
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    // no-op
  }
}
