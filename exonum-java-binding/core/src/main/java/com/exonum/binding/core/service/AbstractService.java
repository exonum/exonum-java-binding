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

package com.exonum.binding.core.service;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.database.View;
import java.util.List;

/**
 * A base class for user services.
 */
public abstract class AbstractService implements Service {

  @Override
  public List<HashCode> getStateHashes(Snapshot snapshot) {
    return createDataSchema(snapshot).getStateHashes();
  }

  /**
   * Creates a data schema of this service.
   *
   * @param view a database view
   * @return a data schema of the service
   */
  protected abstract Schema createDataSchema(View view);
}
