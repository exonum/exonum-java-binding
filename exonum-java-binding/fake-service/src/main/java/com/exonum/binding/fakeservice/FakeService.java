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

package com.exonum.binding.fakeservice;

import com.exonum.binding.service.AbstractService;
import com.exonum.binding.service.Node;
import com.exonum.binding.service.Schema;
import com.exonum.binding.service.TransactionConverter;
import com.exonum.binding.storage.database.View;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import java.util.Collections;

final class FakeService extends AbstractService  {

  @VisibleForTesting
  static final short ID = 1;
  private static final String NAME = "fake-service";

  @Inject
  FakeService(TransactionConverter transactionConverter) {
    super(ID, NAME, transactionConverter);
  }

  @Override
  protected Schema createDataSchema(View view) {
    // No schema
    return Collections::emptyList;
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    // No handlers
  }
}
