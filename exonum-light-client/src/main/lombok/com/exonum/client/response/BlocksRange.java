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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Value;

/**
 * A <em>closed</em> range of blocks. It is allowed to contain «gaps» if blocks containing no
 * transactions are filtered out. The actual range boundaries are accessible with
 * {@link #getFromHeight()} and {@link #getToHeight()}.
 */
@Value
public class BlocksRange {

  /**
   * The height of the first block in the requested range.
   *
   * <p>Please note that in case empty blocks are filtered out, the height of the first block
   * in the {@linkplain #getBlocks() list} might differ from this value.
   */
  long fromHeight;

  /**
   * The height of the last block in the requested range.
   *
   * <p>Please note that in case empty blocks are filtered out, the height of the last block
   * in the {@linkplain #getBlocks() list} might differ from this value.
   */
  long toHeight;

  // todo: I wonder what is better: doing these structures that must document possible «gaps»,
  //   or fully separate normal and excl. empty requests?
  /**
   * Blockchain blocks in ascending order by height. The list is not necessarily continuous if some
   * blocks are filtered out. May be empty if no blocks are found.
   */
  List<Block> blocks;

  /**
   * Creates a new range of blocks.
   *
   * @param fromHeight the height of the first requested block
   * @param toHeight the height of the last requested block
   * @param blocks the list blocks in the given range. It is allowed to <em>not</em> contain
   *     all blocks in the range
   */
  public BlocksRange(long fromHeight, long toHeight, List<Block> blocks) {
    checkArgument(0 <= fromHeight, "fromHeight (%s) is negative");
    checkArgument(fromHeight <= toHeight, "fromHeight (%s) > toHeight (%s)", fromHeight, toHeight);
    this.fromHeight = fromHeight;
    this.toHeight = toHeight;
    this.blocks = checkBlocks(fromHeight, toHeight, blocks);
  }

  private static List<Block> checkBlocks(long fromHeight, long toHeight, List<Block> blocks) {
    if (blocks.isEmpty()) {
      return blocks;
    }
    // Check the first block
    Block first = blocks.get(0);
    checkArgument(fromHeight <= first.getHeight(),
        "First block (%s) appears before fromHeight (%s)", first, fromHeight);

    // Check the last block
    Block last = blocks.get(blocks.size() - 1);
    checkArgument(last.getHeight() <= toHeight,
        "Last block (%s) appears after toHeight (%s)", last, toHeight);

    // Copy (possibly) the list
    return ImmutableList.copyOf(blocks);
  }
}
