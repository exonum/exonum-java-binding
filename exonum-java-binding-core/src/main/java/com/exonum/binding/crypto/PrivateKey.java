package com.exonum.binding.crypto;

import static org.abstractj.kalium.encoders.Encoder.HEX;

public class PrivateKey implements Key {

  private final byte[] privateKey;

  private PrivateKey(byte[] privateKey) {
    this.privateKey = privateKey;
  }

  /**
   * Creates a {@code PrivateKey} from a byte array. The array is defensively copied.
   */
  public static PrivateKey fromBytes(byte[] bytes) {
    return fromBytesNoCopy(bytes.clone());
  }

  /**
   * Creates a {@code PrivateKey} from a byte array. The array is not copied defensively.
   */
  public static PrivateKey fromBytesNoCopy(byte[] bytes) {
    return new PrivateKey(bytes);
  }

  /**
   * Creates a {@code PrivateKey} from a hexadecimal string.
   */
  public static PrivateKey fromHexString(String stringKey) {
    return new PrivateKey(HEX.decode(stringKey));
  }

  @Override
  public byte[] toBytes() {
    return privateKey.clone();
  }

  @Override
  public byte[] toBytesNoCopy() {
    return privateKey;
  }

  @Override
  public String toString() {
    return HEX.encode(privateKey);
  }
}
