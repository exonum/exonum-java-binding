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

package com.exonum.binding.transaction;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.storage.database.Fork;

/**
 * Transaction context class. Contains required information for the transaction execution.
 */
public interface TransactionContext {
  /**
   * Returns database view allowing R/W operations.
   */
  Fork getFork();

  /**
   * Returns SHA-256 hash of the transaction.
   */
  HashCode getTransactionMessageHash();

  /**
   * Returns public key of the transaction author.
   */
  PublicKey getAuthorPk();
}
