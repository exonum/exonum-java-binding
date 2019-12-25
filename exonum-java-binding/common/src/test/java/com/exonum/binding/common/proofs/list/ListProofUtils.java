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
import static com.exonum.binding.common.proofs.list.FlatListProof.BLOB_PREFIX;
import static com.exonum.binding.common.proofs.list.FlatListProof.LIST_BRANCH_PREFIX;
import static com.exonum.binding.common.proofs.list.FlatListProof.LIST_ROOT_PREFIX;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.hash.PrimitiveSink;
import com.google.protobuf.ByteString;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Various utilities for testing ListProof verification.
 */
final class ListProofUtils {

  private ListProofUtils() {
  }

  static HashCode getLeafHashCode(ByteString value) {
    return getLeafHashCode(value.toByteArray());
  }

  static HashCode getLeafHashCode(byte[] value) {
    return Hashing.defaultHashFunction().newHasher()
        .putByte(BLOB_PREFIX)
        .putBytes(value)
        .hash();
  }

  static HashCode getBranchHashCode(HashCode leftHash, @Nullable HashCode rightHashSource) {
    Optional<HashCode> rightHash = Optional.ofNullable(rightHashSource);
    return Hashing.defaultHashFunction().newHasher()
        .putByte(LIST_BRANCH_PREFIX)
        .putObject(leftHash, hashCodeFunnel())
        .putObject(rightHash, (Optional<HashCode> from, PrimitiveSink into) ->
            from.ifPresent((hash) -> hashCodeFunnel().funnel(hash, into)))
        .hash();
  }

  static HashCode getProofListHash(HashCode rootHash, long length) {
    return Hashing.defaultHashFunction().newHasher()
        .putByte(LIST_ROOT_PREFIX)
        .putLong(length)
        .putObject(rootHash, hashCodeFunnel())
        .hash();
  }
}
