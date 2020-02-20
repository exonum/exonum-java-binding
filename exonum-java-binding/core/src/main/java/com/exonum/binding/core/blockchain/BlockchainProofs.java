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

package com.exonum.binding.core.blockchain;

import com.exonum.binding.core.storage.database.Access;
import com.exonum.binding.core.util.LibraryLoader;
import com.exonum.messages.core.Proofs.BlockProof;
import com.exonum.messages.core.Proofs.IndexProof;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Provides constructors of block and index proofs.
 */
final class BlockchainProofs {

  static {
    LibraryLoader.load();
  }

  /**
   * Creates a block proof for the block at the given height.
   *
   * @param access a database access
   * @param height the height of the block
   */
  static BlockProof createBlockProof(Access access, long height) {
    byte[] blockProof = nativeCreateBlockProof(access.getAccessNativeHandle(), height);
    try {
      return BlockProof.parseFrom(blockProof);
    } catch (InvalidProtocolBufferException e) {
      throw new AssertionError("Invalid block proof from native", e);
    }
  }

  /**
   * Creates an index proof for the index with the given full name, as of the given snapshot.
   *
   * @param snapshot a snapshot-based database access
   * @param fullIndexName the full name of a proof index for which to create a proof
   */
  static Optional<IndexProof> createIndexProof(Access snapshot, String fullIndexName) {
    // IndexProof for non-existent index is not supported because it doesn't make sense
    // to combine a proof from an uninitialized index (which is not aggregated) with
    // a proof of absence in the aggregating collection.
    return Optional
        .ofNullable(nativeCreateIndexProof(snapshot.getAccessNativeHandle(), fullIndexName))
        .map(proof -> {
          try {
            return IndexProof.parseFrom(proof);
          } catch (InvalidProtocolBufferException e) {
            throw new AssertionError("Invalid index proof from native", e);
          }
        });
  }

  static native byte[] nativeCreateBlockProof(long accessNativeHandle, long blockHeight);

  /**
   * Creates an index proof for the index with the given full name. If it does not exist
   * or is not Merkelized, returns null.
   *
   * @throws RuntimeException if the access is not Snapshot-based
   */
  @Nullable static native byte[] nativeCreateIndexProof(long accessNativeHandle,
      String fullIndexName);

  private BlockchainProofs() {}
}
