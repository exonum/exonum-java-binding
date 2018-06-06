package com.exonum.binding.crypto;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class AbstractKeyTest {

  @Test
  public void verifyEqualsPublicKey() {
    EqualsVerifier.forClass(AbstractKey.class)
        .withRedefinedSubclass(PublicKey.class)
        .verify();
  }

  @Test
  public void verifyEqualsPrivateKey() {
    EqualsVerifier.forClass(AbstractKey.class)
        .withRedefinedSubclass(PrivateKey.class)
        .verify();
  }
}
