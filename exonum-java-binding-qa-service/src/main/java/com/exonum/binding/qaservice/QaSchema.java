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
import com.exonum.binding.service.Schema;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.MapIndexProxy;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.storage.serialization.StandardSerializers;
import java.util.Collections;
import java.util.List;

/**
 * A schema of the QA service.
 *
 * <p>Has two collections:
 * (a) values of the counters (Merklized),
 * (b) names of the counters.
 */
public final class QaSchema implements Schema {

  /** A namespace of QA service collections. */
  private static final String NAMESPACE = QaService.NAME.replace('-', '_');

  private final View view;

  public QaSchema(View view) {
    this.view = checkNotNull(view);
  }

  @Override
  public List<HashCode> getStateHashes() {
    return Collections.singletonList(counters().getRootHash());
  }

  /**
   * Returns a proof map of counter values.
   */
  public ProofMapIndexProxy<HashCode, Long> counters() {
    String name = fullIndexName("counters");
    return ProofMapIndexProxy.newInstance(name, view, StandardSerializers.hash(),
        StandardSerializers.longs());
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

  private static String fullIndexName(String name) {
    return NAMESPACE + "__" + name;
  }
}
