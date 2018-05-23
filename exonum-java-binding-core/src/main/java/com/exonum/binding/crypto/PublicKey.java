package com.exonum.binding.crypto;

import static org.abstractj.kalium.encoders.Encoder.HEX;

/**
 * Represent a public key in a digital signature system.
 */
public final class PublicKey extends AbstractKey {

  private PublicKey(byte[] publicKey) {
    super(publicKey);
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
  static PublicKey fromBytesNoCopy(byte[] bytes) {
    return new PublicKey(bytes);
  }

  /**
   * Creates a {@code PublicKey} from a hexadecimal string.
   */
  public static PublicKey fromHexString(String stringKey) {
    return new PublicKey(HEX.decode(stringKey));
  }
}
