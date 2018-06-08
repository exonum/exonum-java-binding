package com.exonum.binding.cryptocurrency;

import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_PUBLICKEYBYTES;

import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.test.Bytes;

public class PredefinedOwnerKeys {

  public static final PublicKey firstOwnerKey =
      PublicKey.fromBytes(Bytes.createPrefixed(Bytes.bytes(0), CRYPTO_SIGN_ED25519_PUBLICKEYBYTES));

  public static final PublicKey secondOwnerKey =
      PublicKey.fromBytes(Bytes.createPrefixed(Bytes.bytes(1), CRYPTO_SIGN_ED25519_PUBLICKEYBYTES));

  private PredefinedOwnerKeys() {}
}
