package com.exonum.binding.crypto;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class AbstractKeyTest {

  @Test
  public void verifyEqualsPublicKey() {
    EqualsVerifier.forClass(PublicKey.class)
        .usingGetClass()
        .verify();
  }

  @Test
  public void verifyEqualsPrivateKey() {
    EqualsVerifier.forClass(PrivateKey.class)
        .usingGetClass()
        .verify();
  }
}
