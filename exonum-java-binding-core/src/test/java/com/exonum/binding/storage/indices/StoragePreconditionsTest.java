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
  public void checkStoragePrefixAcceptsNonEmpty() throws Exception {
    byte[] prefix = new byte[]{'p'};

    assertThat(prefix, sameInstance(StoragePreconditions.checkIndexPrefix(prefix)));
  }

  @Test(expected = NullPointerException.class)
  public void checkStoragePrefixDoesNotAcceptNull() throws Exception {
    StoragePreconditions.checkIndexPrefix(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkStoragePrefixDoesNotAcceptEmpty() throws Exception {
    byte[] prefix = new byte[]{};

    StoragePreconditions.checkIndexPrefix(prefix);
  }

  @Test
  public void checkStorageKeyAcceptsEmpty() throws Exception {
    byte[] key = new byte[]{};

    assertThat(key, sameInstance(StoragePreconditions.checkStorageKey(key)));
  }

  @Test
  public void checkStorageKeyAcceptsNonEmpty() throws Exception {
    byte[] key = new byte[]{'k'};

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
    byte[] value = new byte[]{'v'};

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
}
