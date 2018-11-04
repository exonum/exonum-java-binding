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
import com.google.auto.value.AutoValue;
import java.util.OptionalInt;

@AutoValue
public abstract class BlockCommittedEventImpl implements BlockCommittedEvent {

  /**
   * Creates a new block committed event.
   *
   * @param snapshot a snapshot of the blockchain state
   * @param validatorId a validator id. {@code OptionalInt.empty()} if this node is not a validator
   * @param height the current blockchain height
   */
  public static BlockCommittedEventImpl valueOf(
      Snapshot snapshot, OptionalInt validatorId, long height) {
    return new AutoValue_BlockCommittedEventImpl(snapshot, validatorId, height);
  }

  @Override
  public abstract Snapshot getSnapshot();

  @Override
  public abstract OptionalInt getValidatorId();

  @Override
  public abstract long getHeight();

}
