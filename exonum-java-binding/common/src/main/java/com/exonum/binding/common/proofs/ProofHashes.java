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

package com.exonum.binding.common.proofs;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;

public final class ProofHashes {

  /**
   * Checks that the given hash is a SHA-256 hash.
   * @throws IllegalArgumentException if the hash size is not equal to 256 bits
   */
  public static void checkSha256Hash(HashCode hash) {
    int size = hash.bits();
    if (size != Hashing.DEFAULT_HASH_SIZE_BITS) {
      String message = String.format("Invalid hash size (%s), must be 256 bits", size);
      throw new InvalidProofException(message);
    }
  }

  private ProofHashes() {}
}
