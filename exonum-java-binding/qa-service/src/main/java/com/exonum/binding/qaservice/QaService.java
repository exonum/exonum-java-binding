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

package com.exonum.binding.qaservice;

import com.exonum.binding.common.configuration.StoredConfiguration;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.service.Service;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * A simple service for QA purposes.
 */
public interface QaService extends Service {

  short ID = 127;
  String NAME = "qa";

  HashCode submitCreateCounter(String counterName);

  HashCode submitIncrementCounter(long requestSeed, HashCode counterId);

  HashCode submitValidThrowingTx(long requestSeed);

  HashCode submitValidErrorTx(long requestSeed, byte errorCode, @Nullable String description);

  HashCode submitUnknownTx();

  Optional<Counter> getValue(HashCode counterId);

  StoredConfiguration getActualConfiguration();

  Optional<ZonedDateTime> getTime();

  Map<PublicKey, ZonedDateTime> getValidatorsTimes();
}
