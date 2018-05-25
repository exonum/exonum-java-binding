package com.exonum.binding.crypto;

import static org.abstractj.kalium.encoders.Encoder.HEX;

/**
 * Represent a private key in a digital signature system.
 */
public final class PrivateKey extends AbstractKey {

  private PrivateKey(byte[] privateKey) {
    super(privateKey);
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
  static PrivateKey fromBytesNoCopy(byte[] bytes) {
    return new PrivateKey(bytes);
  }

  /**
   * Creates a {@code PrivateKey} from a hexadecimal string.
   */
  public static PrivateKey fromHexString(String stringKey) {
    return new PrivateKey(HEX.decode(stringKey));
  }
}
