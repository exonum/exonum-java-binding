/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.runtime;

import com.exonum.binding.service.AbstractService;
import com.exonum.binding.service.Node;
import com.exonum.binding.service.Schema;
import com.exonum.binding.service.TransactionConverter;
import com.exonum.binding.storage.database.View;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import java.util.Collections;

class TestService extends AbstractService {

  static final short ID = 2;
  static final String NAME = "test-user-service";

  @Inject
  TestService(TransactionConverter transactionConverter) {
    super(ID, NAME, transactionConverter);
  }

  @Override
  protected Schema createDataSchema(View view) {
    return Collections::emptyList;
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    // no-op
  }
}
