/*
 * Copyright 2020 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.common.runtime;

/**
 * Represents well-known runtime ids, as assigned by the Exonum core.
 */
public enum RuntimeId {
  RUST(0),
  JAVA(1);

  private final int id;

  RuntimeId(int id) {
    this.id = id;
  }

  /**
   * Returns the numeric id assigned to this runtime by the Exonum core.
   */
  public int getId() {
    return id;
  }
}
