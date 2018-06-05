package com.exonum.binding.crypto;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class AbstractKeyTest {
  @Test
  public void verifyEquals() {
    EqualsVerifier.forClass(AbstractKey.class)
        .verify();
  }
}
