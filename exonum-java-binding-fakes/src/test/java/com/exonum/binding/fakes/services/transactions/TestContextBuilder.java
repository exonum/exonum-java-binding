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
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.transaction.TransactionContext;

public final class TestContextBuilder {
  private static final HashCode DEFAULT_HASH = HashCode.fromString("a0b0c0d0");
  private static final PublicKey DEFAULT_AUTHOR_KEY = PublicKey.fromHexString("abcd");

  private final TransactionContext.Builder builder;

  private TestContextBuilder(Fork view) {
    this.builder = TransactionContext.builder()
        .fork(view)
        .hash(DEFAULT_HASH)
        .authorPk(DEFAULT_AUTHOR_KEY);
  }

  public static TestContextBuilder newContext(Fork view) {
    return new TestContextBuilder(view);
  }

  public TransactionContext create() {
    return builder.build();
  }

  public TestContextBuilder withHash(HashCode hash) {
    builder.hash(hash);
    return this;
  }

  public TestContextBuilder withAuthorKey(PublicKey key) {
    builder.authorPk(key);
    return this;
  }

}
