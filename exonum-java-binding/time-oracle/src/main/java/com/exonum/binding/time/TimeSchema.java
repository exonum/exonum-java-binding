/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.time;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.EntryIndexProxy;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import java.time.ZonedDateTime;

/**
 * Exonum time service database schema. It provides read-only access to the state
 * of the time oracle for a given {@linkplain View database view}.
 *
 * @see <a href="https://exonum.com/doc/version/latest/advanced/time/">Time oracle documentation</a>
 */
public interface TimeSchema {

  /**
   * Constructs a time schema for a given dbView.
   *
   * <p>Won't be constructed unless time service is enabled. To enable time service, put 'time'
   * into 'ejb_app_services.toml' file.
   *
   * @throws IllegalStateException if time service is not enabled
   */
  static TimeSchema newInstance(View dbView) {
    return new TimeSchemaProxy(dbView);
  }

  /**
   * Returns consolidated time output by the service, which can be used by other business logic on
   * the blockchain.
   *
   * <p>At the time when a new blockchain is launched, the consolidated time is unknown until the
   * transactions from at least two thirds of validator nodes are processed. In that case the result
   * will not contain a value.
   */
  EntryIndexProxy<ZonedDateTime> getTime();

  /**
   * Returns the table that stores time for every validator.
   */
  ProofMapIndexProxy<PublicKey, ZonedDateTime> getValidatorsTimes();
}
