package com.exonum.binding.test;

import static java.util.Arrays.asList;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class TestStorageItems {
  // Auto-generated.
  public static final byte[] K1 = bytes("k1");
  public static final byte[] K2 = bytes("k2");
  public static final byte[] K3 = bytes("k3");
  public static final byte[] K4 = bytes("k4");
  public static final byte[] K5 = bytes("k5");
  public static final byte[] K6 = bytes("k6");
  public static final byte[] K7 = bytes("k7");
  public static final byte[] K8 = bytes("k8");
  public static final byte[] K9 = bytes("k9");
  public static final byte[] K10 = bytes("k10");
  public static final List<byte[]> keys = asList(K1, K2, K3, K4, K5, K6, K7, K8, K9, K10);

  public static final byte[] V1 = bytes("v1");
  public static final byte[] V2 = bytes("v2");
  public static final byte[] V3 = bytes("v3");
  public static final byte[] V4 = bytes("v4");
  public static final byte[] V5 = bytes("v5");
  public static final byte[] V6 = bytes("v6");
  public static final byte[] V7 = bytes("v7");
  public static final byte[] V8 = bytes("v8");
  public static final byte[] V9 = bytes("v9");
  public static final byte[] V10 = bytes("v10");
  public static final List<byte[]> values = asList(V1, V2, V3, V4, V5, V6, V7, V8, V9, V10);

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
}
