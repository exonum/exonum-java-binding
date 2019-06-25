/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.client.request;

/**
 * Request option for filtering blocks.
 */
public enum BlockFilteringOption {
  /**
   * Skip empty blocks (containing no transactions).
   * Only non-empty blocks will be returned in a response.
   *
   * <p>Requesting the <em>server</em> to skip empty blocks when the client does not need them
   * allows to save bandwidth and complete the request faster if a large number of blocks
   * is requested.
   */
  SKIP_EMPTY,
  /**
   * Include all blocks in a response (both empty and non-empty).
   */
  INCLUDE_EMPTY
}
