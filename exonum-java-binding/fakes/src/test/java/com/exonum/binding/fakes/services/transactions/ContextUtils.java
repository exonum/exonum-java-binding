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
 *
 */

package com.exonum.binding.fakes.services.transactions;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.transaction.TransactionContext;

/**
 * Helper class to provide preconfigured context with default values.
 */
public final class ContextUtils {
  private static final HashCode DEFAULT_HASH = HashCode.fromString("a0b0c0d0");
  private static final PublicKey DEFAULT_AUTHOR_KEY = PublicKey.fromHexString("abcd");

  /**
   * Returns new context with default values for a given view.
   */
  public static TransactionContext newContext(Fork view) {
    return TransactionContext.builder()
        .fork(view)
        .txMessageHash(DEFAULT_HASH)
        .authorPk(DEFAULT_AUTHOR_KEY)
        .build();
  }

}
