package com.exonum.binding.crypto;

import com.goterl.lazycode.lazysodium.interfaces.Sign;

public class Crypto {

  public static class Ed25519 {
    public static final int SEED_BYTES = Sign.ED25519_SEEDBYTES;
    public static final int SIGNATURE_BYTES = Sign.ED25519_BYTES;
    public static final int PRIVATE_KEY_BYTES = Sign.SECRETKEYBYTES;
    public static final int PUBLIC_KEY_BYTES = Sign.PUBLICKEYBYTES;
  }

}
