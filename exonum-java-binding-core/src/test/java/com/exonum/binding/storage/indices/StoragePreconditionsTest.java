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

package com.exonum.binding.storage.indices;

import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class StoragePreconditionsTest {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Test
  public void checkIndexNameAcceptsNonEmpty() {
    String name = "table1";

    assertThat(name, sameInstance(StoragePreconditions.checkIndexName(name)));
  }

  @Test(expected = NullPointerException.class)
  public void checkIndexNameDoesNotAcceptNull() {
    StoragePreconditions.checkIndexName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkIndexNameDoesNotAcceptEmpty() {
    String name = "";

    StoragePreconditions.checkIndexName(name);
  }

  @Test
  public void checkIdInGroup() {
    byte[] validId = bytes("id1");

    assertThat(StoragePreconditions.checkIdInGroup(validId), sameInstance(validId));
  }

  @Test
  public void checkIdInGroupNull() {
    expected.expect(NullPointerException.class);
    StoragePreconditions.checkIdInGroup(null);
  }

  @Test
  public void checkIdInGroupEmpty() {
    byte[] emptyId = new byte[0];
    expected.expect(IllegalArgumentException.class);
    StoragePreconditions.checkIdInGroup(emptyId);
  }

  @Test
  public void checkStorageKeyAcceptsEmpty() {
    byte[] key = new byte[]{};

    assertThat(key, sameInstance(StoragePreconditions.checkStorageKey(key)));
  }

  @Test
  public void checkStorageKeyAcceptsNonEmpty() {
    byte[] key = bytes('k');

    assertThat(key, sameInstance(StoragePreconditions.checkStorageKey(key)));
  }

  @Test(expected = NullPointerException.class)
  public void checkStorageKeyDoesNotAcceptNull() {
    StoragePreconditions.checkStorageKey(null);
  }

  @Test
  public void checkProofKeyAccepts32ByteZeroKey() {
    byte[] key = new byte[32];

    assertThat(key, sameInstance(StoragePreconditions.checkProofKey(key)));
  }

  @Test
  public void checkProofKeyAccepts32ByteNonZeroKey() {
    byte[] key = bytes("0123456789abcdef0123456789abcdef");

    assertThat(key, sameInstance(StoragePreconditions.checkProofKey(key)));
  }

  @Test
  public void checkProofKeyDoesNotAcceptNull() {
    expected.expect(NullPointerException.class);
    expected.expectMessage("Proof map key is null");
    StoragePreconditions.checkProofKey(null);
  }

  @Test
  public void checkProofKeyDoesNotAcceptSmallerKeys() {
    byte[] key = new byte[1];

    expected.expect(IllegalArgumentException.class);
    expected.expectMessage("Proof map key has invalid size (1)");
    StoragePreconditions.checkProofKey(key);
  }

  @Test
  public void checkProofKeyDoesNotAcceptBiggerKeys() {
    byte[] key = new byte[64];

    expected.expect(IllegalArgumentException.class);
    expected.expectMessage("Proof map key has invalid size (64)");
    StoragePreconditions.checkProofKey(key);
  }

  @Test
  public void checkStorageValueAcceptsEmpty() {
    byte[] value = new byte[]{};

    assertThat(value, sameInstance(StoragePreconditions.checkStorageValue(value)));
  }

  @Test
  public void checkStorageValueAcceptsNonEmpty() {
    byte[] value = bytes('v');

    assertThat(value, sameInstance(StoragePreconditions.checkStorageValue(value)));
  }

  @Test(expected = NullPointerException.class)
  public void checkStorageValueDoesNotAcceptNull() {
    StoragePreconditions.checkStorageKey(null);
  }

  @Test(expected = NullPointerException.class)
  public void checkNoNulls() {
    Collection<String> c = Arrays.asList("hello", null);

    StoragePreconditions.checkNoNulls(c);
  }

  @Test
  public void checkElementIndexNegative() {
    expected.expect(IndexOutOfBoundsException.class);
    expected.expectMessage("Index must be in range [0, 2), but: -1");

    StoragePreconditions.checkElementIndex(-1, 2);
  }

  @Test
  public void checkElementIndexEqualToSize() {
    expected.expect(IndexOutOfBoundsException.class);
    expected.expectMessage("Index must be in range [0, 2), but: 2");

    StoragePreconditions.checkElementIndex(2, 2);
  }

  @Test
  public void checkElementIndexGreaterThanSize() {
    expected.expect(IndexOutOfBoundsException.class);
    expected.expectMessage("Index must be in range [0, 2), but: 3");

    StoragePreconditions.checkElementIndex(3, 2);
  }

  @Test
  public void checkElementIndexMaxLong() {
    expected.expect(IndexOutOfBoundsException.class);
    expected.expectMessage("Index must be in range [0, 2), but:");

    StoragePreconditions.checkElementIndex(Long.MAX_VALUE, 2);
  }

  @Test
  public void checkElementIndex0MinValid() {
    long index = 0;
    long size = 3;

    assertThat(StoragePreconditions.checkElementIndex(index, size), equalTo(index));
  }

  @Test
  public void checkElementIndex1() {
    long index = 1;
    long size = 3;

    assertThat(StoragePreconditions.checkElementIndex(index, size), equalTo(index));
  }

  @Test
  public void checkElementIndex2MaxValid() {
    long index = 2;
    long size = 3;

    assertThat(StoragePreconditions.checkElementIndex(index, size), equalTo(index));
  }

  @Test
  public void checkPositionIndexSize0_Valid() {
    long index = 0;
    long size = 0;

    assertThat(StoragePreconditions.checkPositionIndex(index, size), equalTo(index));
  }

  @Test
  public void checkPositionIndexSize0_NotValid() {
    long index = 1;
    long size = 0;

    expected.expectMessage("index (1) is greater than size (0)");
    expected.expect(IndexOutOfBoundsException.class);
    StoragePreconditions.checkPositionIndex(index, size);
  }

  @Test
  public void checkPositionIndexSize0_NotValidNegative() {
    long index = -1;
    long size = 0;

    expected.expectMessage("index (-1) is negative");
    expected.expect(IndexOutOfBoundsException.class);
    StoragePreconditions.checkPositionIndex(index, size);
  }

  @Test
  public void checkPositionIndexSize3_AllValid() {
    long size = 3;
    long[] validIndices = {0, 1, 2, 3};

    for (long index : validIndices) {
      assertThat(StoragePreconditions.checkPositionIndex(index, size), equalTo(index));
    }
  }

  @Test
  public void checkPositionIndexSize3_NotValid() {
    long index = 4;
    long size = 3;

    expected.expectMessage("index (4) is greater than size (3)");
    expected.expect(IndexOutOfBoundsException.class);
    StoragePreconditions.checkPositionIndex(index, size);
  }

  @Test
  public void checkPositionIndex_NegativeSize() {
    long index = 0;
    long size = -1;

    expected.expectMessage("size (-1) is negative");
    expected.expect(IllegalArgumentException.class);
    StoragePreconditions.checkPositionIndex(index, size);
  }
}
