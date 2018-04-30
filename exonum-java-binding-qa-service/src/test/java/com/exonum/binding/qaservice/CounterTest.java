package com.exonum.binding.qaservice;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class CounterTest {

  @Test
  public void verifyEquals() {
    EqualsVerifier.forClass(Counter.class)
        .verify();
  }
}
