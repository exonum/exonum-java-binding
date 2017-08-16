package com.exonum.binding.test;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.primitives.UnsignedBytes;
import java.io.UnsupportedEncodingException;

public class Bytes {

  /**
   * Converts a sequence of bytes into an array.
   *
   * @param bytes 0 or more bytes
   * @return an array of bytes
   * @see #bytes(int...)
   */
  public static byte[] bytes(byte... bytes) {
    return bytes;
  }

  /**
   * Converts a sequence of bytes into an array. Accepts integers so that clients do not have
   * to make redundant casts like {@code bytes((byte) 0x0F, (byte) 0xF1)}.
   *
   * @param bytes 0 or more integer values in range [0, 255]
   * @return an array of bytes, each of which has the same binary value
   *         as the least significant byte of the corresponding int in the specified sequence
   * @throws IllegalArgumentException if the bytes contain integers that are not in range [0, 255]
   */
  public static byte[] bytes(int... bytes) {
    byte[] bytesCast = new byte[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      bytesCast[i] = UnsignedBytes.checkedCast(bytes[i]);
    }
    return bytesCast;
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

  /**
   * Creates a byte array of the given size with the specified prefix.
   * Bytes after the prefix are set to zero.
   *
   * @param prefixBytes a prefix of the new array
   * @param size a size of the new array
   * @return a new array that has the prefixBytes as its first elements and zeroes after that
   */
  public static byte[] createPrefixed(byte[] prefixBytes, int size) {
    checkArgument(0 <= size, "Size is negative: %s", size);
    checkArgument(prefixBytes.length <= size,
        "Prefix size (%s) is greater than the specified size (%s)", prefixBytes.length, size);

    byte[] key = new byte[size];
    System.arraycopy(prefixBytes, 0, key, 0, prefixBytes.length);
    return key;
  }

  private Bytes() {}
}
