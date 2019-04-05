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

import com.exonum.binding.service.AbstractServiceModule;
import com.exonum.binding.service.Service;
import com.exonum.binding.service.TransactionConverter;
import com.google.inject.Singleton;
import org.pf4j.Extension;

/**
 * A module configuring {@link FakeService}.
 */
@Extension
public final class FakeServiceModule extends AbstractServiceModule {

  private static final TransactionConverter THROWING_TX_CONVERTER = (tx) -> {
    throw new IllegalStateException("No transactions in this service: " + tx);
  };

  @Override
  protected void configure() {
    bind(Service.class).to(FakeService.class)
        .in(Singleton.class);
    bind(TransactionConverter.class).toInstance(THROWING_TX_CONVERTER);
  }
}
