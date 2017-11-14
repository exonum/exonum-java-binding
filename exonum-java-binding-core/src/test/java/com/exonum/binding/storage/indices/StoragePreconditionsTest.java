package com.exonum.binding.storage.indices;

import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class StoragePreconditionsTest {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Test
  public void checkIndexNameAcceptsNonEmpty() throws Exception {
    String name = "table1";

    assertThat(name, sameInstance(StoragePreconditions.checkIndexName(name)));
  }

  @Test(expected = NullPointerException.class)
  public void checkIndexNameDoesNotAcceptNull() throws Exception {
    StoragePreconditions.checkIndexName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkIndexNameDoesNotAcceptEmpty() throws Exception {
    String name = "";

    StoragePreconditions.checkIndexName(name);
  }

  @Test
  public void checkStorageKeyAcceptsEmpty() throws Exception {
    byte[] key = new byte[]{};

    assertThat(key, sameInstance(StoragePreconditions.checkStorageKey(key)));
  }

  @Test
  public void checkStorageKeyAcceptsNonEmpty() throws Exception {
    byte[] key = bytes('k');

    assertThat(key, sameInstance(StoragePreconditions.checkStorageKey(key)));
  }

  @Test(expected = NullPointerException.class)
  public void checkStorageKeyDoesNotAcceptNull() throws Exception {
    StoragePreconditions.checkStorageKey(null);
  }

  @Test
  public void checkProofKeyAccepts32ByteZeroKey() throws Exception {
    byte[] key = new byte[32];

    assertThat(key, sameInstance(StoragePreconditions.checkProofKey(key)));
  }

  @Test
  public void checkProofKeyAccepts32ByteNonZeroKey() throws Exception {
    byte[] key = bytes("0123456789abcdef0123456789abcdef");

    assertThat(key, sameInstance(StoragePreconditions.checkProofKey(key)));
  }

  @Test
  public void checkProofKeyDoesNotAcceptNull() throws Exception {
    expected.expect(NullPointerException.class);
    expected.expectMessage("Proof map key is null");
    StoragePreconditions.checkProofKey(null);
  }

  @Test
  public void checkProofKeyDoesNotAcceptSmallerKeys() throws Exception {
    byte[] key = new byte[1];

    expected.expect(IllegalArgumentException.class);
    expected.expectMessage("Proof map key has invalid size: 1");
    StoragePreconditions.checkProofKey(key);
  }

  @Test
  public void checkProofKeyDoesNotAcceptBiggerKeys() throws Exception {
    byte[] key = new byte[64];

    expected.expect(IllegalArgumentException.class);
    expected.expectMessage("Proof map key has invalid size: 64");
    StoragePreconditions.checkProofKey(key);
  }

  @Test
  public void checkStorageValueAcceptsEmpty() throws Exception {
    byte[] value = new byte[]{};

    assertThat(value, sameInstance(StoragePreconditions.checkStorageValue(value)));
  }

  @Test
  public void checkStorageValueAcceptsNonEmpty() throws Exception {
    byte[] value = bytes('v');

    assertThat(value, sameInstance(StoragePreconditions.checkStorageValue(value)));
  }

  @Test(expected = NullPointerException.class)
  public void checkStorageValueDoesNotAcceptNull() throws Exception {
    StoragePreconditions.checkStorageKey(null);
  }

  @Test
  public void checkElementIndexNegative() throws Exception {
    expected.expect(IndexOutOfBoundsException.class);
    expected.expectMessage("Index must be in range [0, 2), but: -1");

    StoragePreconditions.checkElementIndex(-1, 2);
  }

  @Test
  public void checkElementIndexEqualToSize() throws Exception {
    expected.expect(IndexOutOfBoundsException.class);
    expected.expectMessage("Index must be in range [0, 2), but: 2");

    StoragePreconditions.checkElementIndex(2, 2);
  }

  @Test
  public void checkElementIndexGreaterThanSize() throws Exception {
    expected.expect(IndexOutOfBoundsException.class);
    expected.expectMessage("Index must be in range [0, 2), but: 3");

    StoragePreconditions.checkElementIndex(3, 2);
  }

  @Test
  public void checkElementIndexMaxLong() throws Exception {
    expected.expect(IndexOutOfBoundsException.class);
    expected.expectMessage("Index must be in range [0, 2), but:");

    StoragePreconditions.checkElementIndex(Long.MAX_VALUE, 2);
  }

  @Test
  public void checkElementIndex0MinValid() throws Exception {
    long index = 0;
    long size = 3;

    assertThat(StoragePreconditions.checkElementIndex(index, size), equalTo(index));
  }

  @Test
  public void checkElementIndex1() throws Exception {
    long index = 1;
    long size = 3;

    assertThat(StoragePreconditions.checkElementIndex(index, size), equalTo(index));
  }

  @Test
  public void checkElementIndex2MaxValid() throws Exception {
    long index = 2;
    long size = 3;

    assertThat(StoragePreconditions.checkElementIndex(index, size), equalTo(index));
  }

  @Test
  public void checkPositionIndexSize0_Valid() throws Exception {
    long index = 0;
    long size = 0;

    assertThat(StoragePreconditions.checkPositionIndex(index, size), equalTo(index));
  }

  @Test
  public void checkPositionIndexSize0_NotValid() throws Exception {
    long index = 1;
    long size = 0;

    expected.expectMessage("index (1) is greater than size (0)");
    expected.expect(IndexOutOfBoundsException.class);
    StoragePreconditions.checkPositionIndex(index, size);
  }

  @Test
  public void checkPositionIndexSize0_NotValidNegative() throws Exception {
    long index = -1;
    long size = 0;

    expected.expectMessage("index (-1) is negative");
    expected.expect(IndexOutOfBoundsException.class);
    StoragePreconditions.checkPositionIndex(index, size);
  }

  @Test
  public void checkPositionIndexSize3_AllValid() throws Exception {
    long size = 3;
    long[] validIndices = {0, 1, 2, 3};

    for (long index : validIndices) {
      assertThat(StoragePreconditions.checkPositionIndex(index, size), equalTo(index));
    }
  }

  @Test
  public void checkPositionIndexSize3_NotValid() throws Exception {
    long index = 4;
    long size = 3;

    expected.expectMessage("index (4) is greater than size (3)");
    expected.expect(IndexOutOfBoundsException.class);
    StoragePreconditions.checkPositionIndex(index, size);
  }

  @Test
  public void checkPositionIndex_NegativeSize() throws Exception {
    long index = 0;
    long size = -1;

    expected.expectMessage("size (-1) is negative");
    expected.expect(IllegalArgumentException.class);
    StoragePreconditions.checkPositionIndex(index, size);
  }
}
