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

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.service.Schema;
import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.core.storage.indices.EntryIndexProxy;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.core.storage.indices.MapIndexProxy;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.time.TimeSchema;
import java.util.Collections;
import java.util.List;

/**
 * A schema of the QA service.
 *
 * <p>Has two collections:
 * (a) values of the counters (Merkelized),
 * (b) names of the counters.
 */
public final class QaSchema implements Schema {

  private final View view;
  /** A namespace of QA service collections. */
  private final String namespace;

  public QaSchema(View view, String serviceName) {
    this.view = checkNotNull(view);
    namespace = serviceName;
  }

  @Override
  public List<HashCode> getStateHashes() {
    return Collections.singletonList(counters().getIndexHash());
  }

  /**
   * Returns the index containing the name of the time oracle to use.
   */
  public EntryIndexProxy<String> timeOracleName() {
    String name = fullIndexName("time_oracle_name");
    return EntryIndexProxy.newInstance(name, view, StandardSerializers.string());
  }

  /**
   * Returns the time schema of the time oracle this qa service uses.
   * {@link #timeOracleName()} must be non-empty.
   */
  public TimeSchema timeSchema() {
    return TimeSchema.newInstance(view, timeOracleName().get());
  }

  /**
   * Returns a proof map of counter values. Note that this is a
   * <a href="ProofMapIndexProxy.html#key-hashing">proof map that uses non-hashed keys</a>.
   */
  public ProofMapIndexProxy<HashCode, Long> counters() {
    String name = fullIndexName("counters");
    return ProofMapIndexProxy.newInstanceNoKeyHashing(name, view, StandardSerializers.hash(),
        StandardSerializers.uint64());
  }

  /**
   * Returns a map of counter names.
   */
  public MapIndex<HashCode, String> counterNames() {
    String name = fullIndexName("counterNames");
    return MapIndexProxy.newInstance(name, view, StandardSerializers.hash(),
        StandardSerializers.string());
  }

  /** Clears all collections of the service. */
  public void clearAll() {
    counters().clear();
    counterNames().clear();
  }

  private String fullIndexName(String name) {
    return namespace + "." + name;
  }
}
