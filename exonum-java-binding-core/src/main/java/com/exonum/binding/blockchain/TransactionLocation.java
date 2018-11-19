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

package com.exonum.binding.blockchain;

import com.google.common.base.Objects;

/**
 * Transaction position in a block.
 */
class TransactionLocation {

  private long height;
  private long indexInBlock;

  TransactionLocation(long height, long indexInBlock) {
    this.height = height;
    this.indexInBlock = indexInBlock;
  }

  /**
   * Height of the block where the transaction was included.
   */
  public long getHeight() {
    return height;
  }

  /**
   * Zero-based position of this transaction in the block.
   */
  public long getIndexInBlock() {
    return indexInBlock;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TransactionLocation that = (TransactionLocation) o;
    return height == that.height &&
        indexInBlock == that.indexInBlock;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(height, indexInBlock);
  }

  @Override
  public String toString() {
    return "TransactionLocation{" +
        "height=" + height +
        ", indexInBlock=" + indexInBlock +
        '}';
  }

}
