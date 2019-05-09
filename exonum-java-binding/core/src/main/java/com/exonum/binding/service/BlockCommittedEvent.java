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

package com.exonum.binding.service;

import com.exonum.binding.storage.database.Snapshot;
import java.util.OptionalInt;

/**
 * The blockchain state just after the corresponding block is committed.
 * This structure is passed to the {@link Service#afterCommit(BlockCommittedEvent)} method
 * and is used for the interaction between service business logic and the blockchain state.
 */
public interface BlockCommittedEvent {

  /**
   * If this node is a <a href="https://exonum.com/doc/version/0.11/glossary/#validator">validator</a>,
   * returns its identifier.
   * If this node is an <a href="https://exonum.com/doc/version/0.11/glossary/#auditor">auditor</a>,
   * it will return {@code OptionalInt.empty()}.
   */
  OptionalInt getValidatorId();

  /**
   * Returns the current blockchain height, which is the height of the last committed block.
   */
  long getHeight();

  /**
   * Returns the current database snapshot. It is immutable and represents the database state
   * as of the block at the current {@linkplain #getHeight() height}.
   */
  Snapshot getSnapshot();
}
