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

package com.exonum.binding.cryptocurrency;

import com.exonum.binding.core.service.AbstractServiceModule;
import com.exonum.binding.core.service.Service;
import com.exonum.binding.core.service.TransactionConverter;
import com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionConverter;
import com.google.inject.Singleton;
import org.pf4j.Extension;

@Extension
public final class CryptocurrencyServiceModule extends AbstractServiceModule {

  @Override
  protected void configure() {
    bind(Service.class).to(CryptocurrencyServiceImpl.class);
    bind(CryptocurrencyService.class).to(CryptocurrencyServiceImpl.class).in(Singleton.class);
    bind(TransactionConverter.class).to(CryptocurrencyTransactionConverter.class);
  }
}
