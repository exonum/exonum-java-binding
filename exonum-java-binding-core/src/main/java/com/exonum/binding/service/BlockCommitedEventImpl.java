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
import java.util.Optional;

public class BlockCommitedEventImpl implements BlockCommittedEvent {

  private int validatorId;
  private long height;
  private Snapshot snapshot;

  /**
   * Creates a new block commited event.
   * @param snapshot a snapshot of the blockchain state
   * @param validatorId a validator id. Negative if this node is not a validator
   * @param height the current blockchain height
   */
  public BlockCommitedEventImpl(Snapshot snapshot, int validatorId, long height) {
    this.validatorId = validatorId;
    this.height = height;
    this.snapshot = snapshot;
  }

  @Override
  public Snapshot getSnapshot() {
    return snapshot;
  }

  @Override
  public Optional<Integer> getValidatorId() {
    return validatorId > 0 ? Optional.of(validatorId) : Optional.empty();
  }

  @Override
  public long getHeight() {
    return height;
  }

}
