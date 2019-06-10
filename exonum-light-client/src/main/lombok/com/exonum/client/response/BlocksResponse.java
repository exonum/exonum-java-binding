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

// todo: remove
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
   * The largest height of the returned blocks that match the search criteria, <em>plus one</em>.
   *
   * <p><b>WARNING: do not rely on the value, because it's unpredictable in some queries.</b>
   * It'll be fixed in future releases. Read below if would still like to use it.
   *
   * <p>The value is always equal to {@code blocks[0].height + 1} for responses with blocks.
   * For responses without blocks the value is set mostly randomly depending on request parameters
   * and blockchain state:
   * The value is equal to {@code heightMax + 1} if heightMax request parameter is passed.
   * If the heightMax request parameter is greater then last committed block height at the moment
   * then blocksRangeEnd value will be equal to {@code last_committed_block.height + 1}.
   * If some blocks in the range do not match the search criteria then:
   * {@code blocksRangeEnd - blocksRangeStart != blocks.size}.
   */
  long blocksRangeEnd;
}
