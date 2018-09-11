package com.exonum.binding.common.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CryptoUtilsTest {

  @Test
  public void hasLength() {
    byte[] bytes = new byte[10];

    assertTrue(CryptoUtils.hasLength(bytes, 10));
  }

  @Test
  public void hexToByteArray() {
    byte[] bytes = CryptoUtils.hexToByteArray("abcd");

    assertEquals(2, bytes.length);
    assertEquals(-85, bytes[0]);
    assertEquals(-51, bytes[1]);
  }

  @Test
  public void byteArrayToHex() {
    String hex = CryptoUtils.byteArrayToHex(new byte[]{-85, -51});

    assertEquals("abcd", hex);
  }

}
