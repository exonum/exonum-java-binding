package com.exonum.binding.storage.indices;

import static com.exonum.binding.test.Bytes.bytes;
import static java.util.Arrays.asList;

import java.util.List;

final class TestStorageItems {
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

  /**
   * Lexicographically ordered storage keys.
   */
  public static final List<byte[]> keys = asList(K1, K2, K3, K4, K5, K6, K7, K8, K9);

  public static final byte[] V1 = bytes("v1");
  public static final byte[] V2 = bytes("v2");
  public static final byte[] V3 = bytes("v3");
  public static final byte[] V4 = bytes("v4");
  public static final byte[] V5 = bytes("v5");
  public static final byte[] V6 = bytes("v6");
  public static final byte[] V7 = bytes("v7");
  public static final byte[] V8 = bytes("v8");
  public static final byte[] V9 = bytes("v9");

  /**
   * Storage values.
   */
  public static final List<byte[]> values = asList(V1, V2, V3, V4, V5, V6, V7, V8, V9);

  private TestStorageItems() {}
}
