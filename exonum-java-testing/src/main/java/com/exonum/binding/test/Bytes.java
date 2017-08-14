package com.exonum.binding.test;

import java.io.UnsupportedEncodingException;

public class Bytes {

  public static byte[] bytes(byte... bytes) {
    return bytes;
  }

  /**
   * Converts a string to a sequence of bytes using UTF-8 charset.
   * @param s a string to convert.
   * @return a string as bytes in UTF-8.
   */
  public static byte[] bytes(String s) {
    try {
      return s.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  private Bytes() {}
}
