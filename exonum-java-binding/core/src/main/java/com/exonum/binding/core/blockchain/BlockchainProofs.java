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

import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.core.util.LibraryLoader;
import com.exonum.core.messages.Proofs.BlockProof;
import com.exonum.core.messages.Proofs.IndexProof;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Provides constructors of block and index proofs.
 */
final class BlockchainProofs {

  static {
    LibraryLoader.load();
  }

  /**
   * Creates a block proof for the block at the given height.
   * @param view a database view
   * @param height the height of the block
   */
  static BlockProof createBlockProof(
      /* todo: here snapshot is not strictly required — but shall we allow Forks (see the ticket) */
      View view,
      long height) {
    byte[] blockProof = nativeCreateBlockProof(view.getViewNativeHandle(), height);
    try {
      return BlockProof.parseFrom(blockProof);
    } catch (InvalidProtocolBufferException e) {
      throw new AssertionError("Invalid block proof from native", e);
    }
  }

  /**
   * Creates an index proof for the index with the given full name, as of the given snapshot.
   * @param snapshot a database snapshot
   * @param fullIndexName the full name of a proof index for which to create a proof
   */
  static IndexProof createIndexProof(Snapshot snapshot, String fullIndexName) {
    byte[] indexProof = nativeCreateIndexProof(snapshot.getViewNativeHandle(), fullIndexName);
    try {
      return IndexProof.parseFrom(indexProof);
    } catch (InvalidProtocolBufferException e) {
      throw new AssertionError("Invalid index proof from native", e);
    }
  }

  static native byte[] nativeCreateBlockProof(long viewNativeHandle, long blockHeight);

  static native byte[] nativeCreateIndexProof(long snapshotNativeHandle, String fullIndexName);

  private BlockchainProofs() {}
}
