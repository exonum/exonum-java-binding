/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.service;

import static com.exonum.binding.messages.TemplateMessage.TEMPLATE_MESSAGE;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.service.adapters.UserServiceAdapter;
import com.exonum.binding.service.adapters.UserTransactionAdapter;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.transaction.AbstractTransaction;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ServiceBootstrapIntegrationTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void startService() {
    UserServiceAdapter service = ServiceBootstrap.startService(
        UserModule.class.getCanonicalName(), 0);

    // Check the service and its dependencies work as expected.
    assertThat(service.getId(), equalTo(UserService.ID));
    assertThat(service.getName(), equalTo(UserService.NAME));
    BinaryMessage message = new Message.Builder()
        .mergeFrom(TEMPLATE_MESSAGE)
        .setServiceId(service.getId())
        .buildRaw();
    byte[] messageBytes = message.getSignedMessage().array();

    UserTransactionAdapter transactionAdapter = service.convertTransaction(messageBytes);
    assertTrue(transactionAdapter.isValid());

    // Check that once startService returns, the native library is loaded. If it’s not,
    // we’ll get an UnsatisfiedLinkError.
    try (MemoryDb database = MemoryDb.newInstance()) {
      assertNotNull(database);
    }
  }

  @Test
  public void startServiceNotModule() {
    String invalidModuleName = Object.class.getCanonicalName();

    expectedException.expectMessage("class java.lang.Object is not a sub-class "
        + "of com.google.inject.Module");
    expectedException.expect(IllegalArgumentException.class);
    ServiceBootstrap.startService(invalidModuleName, 0);
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
    return new Schema() {

      @Override
      public List<HashCode> getStateHashes() {
        return Collections.emptyList();
      }
    };
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    // no-op
  }
}
