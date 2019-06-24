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

package com.exonum.binding.core.storage.indices;

import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.Test;

class StoragePreconditionsTest {

  @Test
  void checkIndexNameAcceptsNonEmpty() {
    String name = "table1";

    assertThat(name, sameInstance(StoragePreconditions.checkIndexName(name)));
  }

  @Test
  void checkIndexNameDoesNotAcceptNull() {
    assertThrows(NullPointerException.class, () -> StoragePreconditions.checkIndexName(null));
  }

  @Test
  void checkIndexNameDoesNotAcceptEmpty() {
    assertThrows(IllegalArgumentException.class, () -> {
      String name = "";

      StoragePreconditions.checkIndexName(name);
    });
  }

  @Test
  void checkIdInGroup() {
    byte[] validId = bytes("id1");

    assertThat(StoragePreconditions.checkIdInGroup(validId), sameInstance(validId));
  }

  @Test
  void checkIdInGroupNull() {
    assertThrows(NullPointerException.class, () -> StoragePreconditions.checkIdInGroup(null));
  }

  @Test
  void checkIdInGroupEmpty() {
    byte[] emptyId = new byte[0];

    assertThrows(IllegalArgumentException.class,
        () -> StoragePreconditions.checkIdInGroup(emptyId));
  }

  @Test
  void checkStorageKeyAcceptsEmpty() {
    byte[] key = new byte[]{};

    assertThat(key, sameInstance(StoragePreconditions.checkStorageKey(key)));
  }

  @Test
  void checkStorageKeyAcceptsNonEmpty() {
    byte[] key = bytes('k');

    assertThat(key, sameInstance(StoragePreconditions.checkStorageKey(key)));
  }

  @Test
  void checkStorageKeyDoesNotAcceptNull() {
    assertThrows(NullPointerException.class, () -> StoragePreconditions.checkStorageKey(null));
  }

  @Test
  void checkProofKeyAccepts32ByteZeroKey() {
    byte[] key = new byte[32];

    assertThat(key, sameInstance(StoragePreconditions.checkProofKey(key)));
  }

  @Test
  void checkProofKeyAccepts32ByteNonZeroKey() {
    byte[] key = bytes("0123456789abcdef0123456789abcdef");

    assertThat(key, sameInstance(StoragePreconditions.checkProofKey(key)));
  }

  @Test
  void checkProofKeyDoesNotAcceptNull() {
    NullPointerException thrown = assertThrows(NullPointerException.class,
        () -> StoragePreconditions.checkProofKey(null));
    assertThat(thrown.getLocalizedMessage(), containsString("Proof map key is null"));
  }

  @Test
  void checkProofKeyDoesNotAcceptSmallerKeys() {
    byte[] key = new byte[1];

    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> StoragePreconditions.checkProofKey(key));
    assertThat(thrown.getLocalizedMessage(), containsString("Proof map key has invalid size (1)"));
  }

  @Test
  void checkProofKeyDoesNotAcceptBiggerKeys() {
    byte[] key = new byte[64];

    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> StoragePreconditions.checkProofKey(key));
    assertThat(thrown.getLocalizedMessage(), containsString("Proof map key has invalid size (64)"));
  }

  @Test
  void checkStorageValueAcceptsEmpty() {
    byte[] value = new byte[]{};

    assertThat(value, sameInstance(StoragePreconditions.checkStorageValue(value)));
  }

  @Test
  void checkStorageValueAcceptsNonEmpty() {
    byte[] value = bytes('v');

    assertThat(value, sameInstance(StoragePreconditions.checkStorageValue(value)));
  }

  @Test
  void checkStorageValueDoesNotAcceptNull() {
    assertThrows(NullPointerException.class, () -> StoragePreconditions.checkStorageKey(null));
  }

  @Test
  void checkNoNulls() {
    assertThrows(NullPointerException.class, () -> {
      Collection<String> c = Arrays.asList("hello", null);

      StoragePreconditions.checkNoNulls(c);
    });
  }

  @Test
  void checkElementIndexNegative() {
    IndexOutOfBoundsException thrown = assertThrows(IndexOutOfBoundsException.class,
        () -> StoragePreconditions.checkElementIndex(-1, 2));
    assertThat(thrown.getLocalizedMessage(),
        containsString("Index must be in range [0, 2), but: -1"));
  }

  @Test
  void checkElementIndexEqualToSize() {
    IndexOutOfBoundsException thrown = assertThrows(IndexOutOfBoundsException.class,
        () -> StoragePreconditions.checkElementIndex(2, 2));
    assertThat(thrown.getLocalizedMessage(),
        containsString("Index must be in range [0, 2), but: 2"));
  }

  @Test
  void checkElementIndexGreaterThanSize() {
    IndexOutOfBoundsException thrown = assertThrows(IndexOutOfBoundsException.class,
        () -> StoragePreconditions.checkElementIndex(3, 2));
    assertThat(thrown.getLocalizedMessage(),
        containsString("Index must be in range [0, 2), but: 3"));
  }

  @Test
  void checkElementIndexMaxLong() {
    IndexOutOfBoundsException thrown = assertThrows(IndexOutOfBoundsException.class,
        () -> StoragePreconditions.checkElementIndex(Long.MAX_VALUE, 2));
    assertThat(thrown.getLocalizedMessage(), containsString("Index must be in range [0, 2), but:"));
  }

  @Test
  void checkElementIndex0MinValid() {
    long index = 0;
    long size = 3;

    assertThat(StoragePreconditions.checkElementIndex(index, size), equalTo(index));
  }

  @Test
  void checkElementIndex1() {
    long index = 1;
    long size = 3;

    assertThat(StoragePreconditions.checkElementIndex(index, size), equalTo(index));
  }

  @Test
  void checkElementIndex2MaxValid() {
    long index = 2;
    long size = 3;

    assertThat(StoragePreconditions.checkElementIndex(index, size), equalTo(index));
  }

  @Test
  void checkPositionIndexSize0_Valid() {
    long index = 0;
    long size = 0;

    assertThat(StoragePreconditions.checkPositionIndex(index, size), equalTo(index));
  }

  @Test
  void checkPositionIndexSize0_NotValid() {
    long index = 1;
    long size = 0;

    IndexOutOfBoundsException thrown = assertThrows(IndexOutOfBoundsException.class,
        () -> StoragePreconditions.checkPositionIndex(index, size));
    assertThat(thrown.getLocalizedMessage(), containsString("index (1) is greater than size (0)"));
  }

  @Test
  void checkPositionIndexSize0_NotValidNegative() {
    long index = -1;
    long size = 0;

    IndexOutOfBoundsException thrown = assertThrows(IndexOutOfBoundsException.class,
        () -> StoragePreconditions.checkPositionIndex(index, size));
    assertThat(thrown.getLocalizedMessage(), containsString("index (-1) is negative"));
  }

  @Test
  void checkPositionIndexSize3_AllValid() {
    long size = 3;
    long[] validIndices = {0, 1, 2, 3};

    for (long index : validIndices) {
      assertThat(StoragePreconditions.checkPositionIndex(index, size), equalTo(index));
    }
  }

  @Test
  void checkPositionIndexSize3_NotValid() {
    long index = 4;
    long size = 3;

    IndexOutOfBoundsException thrown = assertThrows(IndexOutOfBoundsException.class,
        () -> StoragePreconditions.checkPositionIndex(index, size));
    assertThat(thrown.getLocalizedMessage(), containsString("index (4) is greater than size (3)"));
  }

  @Test
  void checkPositionIndex_NegativeSize() {
    long index = 0;
    long size = -1;

    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> StoragePreconditions.checkPositionIndex(index, size));
    assertThat(thrown.getLocalizedMessage(), containsString("size (-1) is negative"));
  }
}
