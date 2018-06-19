/* 
 * Copyright 2018 The Exonum Team
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

package com.exonum.binding.storage.proofs;

import com.exonum.binding.hash.Funnel;
import com.exonum.binding.hash.PrimitiveSink;
import com.exonum.binding.storage.proofs.map.DbKey;

/**
 * A funnel for a database key. Puts the raw database key (34 bytes) into the sink.
 */
public enum DbKeyFunnel implements Funnel<DbKey> {
  INSTANCE;

  @Override
  public void funnel(DbKey from, PrimitiveSink into) {
    into.putBytes(from.getRawDbKey());
  }

  public static Funnel<DbKey> dbKeyFunnel() {
    return INSTANCE;
  }
}
