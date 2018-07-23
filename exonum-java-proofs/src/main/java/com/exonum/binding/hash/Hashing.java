/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.hash;

/**
 * Static methods to obtain {@link HashFunction} instances, and other static hashing-related
 * utilities.
 *
 * <p>A comparison of the various hash functions can be found
 * <a href="http://goo.gl/jS7HH">here</a>.
 *
 * @author Kevin Bourrillion
 * @author Dimitris Andreou
 * @author Kurt Alfred Kluever
 * @since 11.0
 */
public final class Hashing {

  /**
   * Size of a hash code in the default Exonum algorithm.
   */
  public static final int DEFAULT_HASH_SIZE_BYTES = 32;
  /**
   * Size of a hash code in the default Exonum algorithm, in bits.
   */
  public static final int DEFAULT_HASH_SIZE_BITS = DEFAULT_HASH_SIZE_BYTES * Byte.SIZE;

  /** Returns a hash function implementing the SHA-256 algorithm (256 hash bits). */
  public static HashFunction sha256() {
    return Sha256Holder.SHA_256;
  }

  /**
   * Returns the default Exonum hash function: SHA-256.
   *
   * @see HashFunction#newHasher()
   */
  public static HashFunction defaultHashFunction() {
    return sha256();
  }

  private static class Sha256Holder {

    static final HashFunction SHA_256 =
        new MessageDigestHashFunction("SHA-256", "Hashing.sha256()");
  }
  /**
   * Returns a hash function implementing the SHA-384 algorithm (384 hash bits).
   *
   * @since 19.0
   */
  public static HashFunction sha384() {
    return Sha384Holder.SHA_384;
  }

  private static class Sha384Holder {

    static final HashFunction SHA_384 =
        new MessageDigestHashFunction("SHA-384", "Hashing.sha384()");
  }
  /** Returns a hash function implementing the SHA-512 algorithm (512 hash bits). */
  public static HashFunction sha512() {
    return Sha512Holder.SHA_512;
  }

  private static class Sha512Holder {

    static final HashFunction SHA_512 =
        new MessageDigestHashFunction("SHA-512", "Hashing.sha512()");
  }

  private Hashing() {}
}
