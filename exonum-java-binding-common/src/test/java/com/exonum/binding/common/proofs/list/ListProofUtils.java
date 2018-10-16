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

package com.exonum.binding.common.proofs.list;

import static com.exonum.binding.common.hash.Funnels.hashCodeFunnel;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.hash.PrimitiveSink;
import com.exonum.binding.common.serialization.StandardSerializers;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Various utilities for testing ListProof verification {@link ListProofNode}s.
 */
final class ListProofUtils {

  private ListProofUtils() {
  }

  /**
   * Creates ListProof element node.
   */
  static ListProofElement leafOf(String element) {
    byte[] dbElement = bytesOf(element);
    return new ListProofElement(dbElement);
  }

  /**
   * Returns string bytes. Standard string serializer is used.
   */
  static byte[] bytesOf(String element) {
    return StandardSerializers.string().toBytes(element);
  }

  /**
   * Generates right leaning proof tree of specified depth.
   */
  static ListProofNode generateRightLeaningProofTree(int depth, ListProofNode leafNode) {
    ListProofNode root = null;
    ListProofNode left = leafNode;
    HashCode h1 = HashCode.fromString("a1");

    int d = depth;
    while (d != 0) {
      ListProofNode right = new ListProofHashNode(h1);
      root = new ListProofBranch(left, right);
      left = root;
      d--;
    }
    return root;
  }

  static HashCode getNodeHashCode(String v1) {
    return Hashing.defaultHashFunction().newHasher()
        .putString(v1, StandardCharsets.UTF_8)
        .hash();
  }

  static HashCode getBranchHashCode(HashCode leftHash, @Nullable HashCode rightHashSource) {
    Optional<HashCode> rightHash = Optional.ofNullable(rightHashSource);
    return Hashing.defaultHashFunction().newHasher()
        .putObject(leftHash, hashCodeFunnel())
        .putObject(rightHash, (Optional<HashCode> from, PrimitiveSink into) ->
            from.ifPresent((hash) -> hashCodeFunnel().funnel(hash, into)))
        .hash();
  }
}
