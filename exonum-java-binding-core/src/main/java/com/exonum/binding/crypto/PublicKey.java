package com.exonum.binding.crypto;

import static org.abstractj.kalium.encoders.Encoder.HEX;

public class PublicKey implements Key {

  private final byte[] publicKey;

  private PublicKey(byte[] publicKey) {
    this.publicKey = publicKey;
  }

  /**
   * Creates a {@code PublicKey} from a byte array. The array is defensively copied.
   */
  public static PublicKey fromBytes(byte[] bytes) {
    return fromBytesNoCopy(bytes.clone());
  }

  /**
   * Creates a {@code PublicKey} from a byte array. The array is not copied defensively.
   */
  public static PublicKey fromBytesNoCopy(byte[] bytes) {
    return new PublicKey(bytes);
  }

  /**
   * Creates a {@code PublicKey} from a hexadecimal string.
   */
  public static PublicKey fromHexString(String stringKey) {
    return new PublicKey(HEX.decode(stringKey));
  }

  @Override
  public byte[] toBytes() {
    return publicKey.clone();
  }

  @Override
  public byte[] toBytesNoCopy() {
    return publicKey;
  }

  @Override
  public String toString() {
    return HEX.encode(publicKey);
  }
}
