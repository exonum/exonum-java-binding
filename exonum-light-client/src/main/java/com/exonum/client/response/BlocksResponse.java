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

package com.exonum.client.response;

import java.util.List;
import lombok.Value;

@Value
public class BlocksResponse {
  /**
   * Blockchain blocks in descending order (not necessarily continuous) by height.
   * It is allowed to be empty if no blocks found.
   */
  List<Block> blocks;

  /**
   * The smallest height of the returned blocks that match the search criteria.
   */
  long blocksRangeStart;

  /**
   * The largest height of the returned blocks that match the search criteria.
   * if blocks have gaps then: {@code blocksRangeEnd - blocksRangeStart != blocks.size}
   */
  long blocksRangeEnd;
}
