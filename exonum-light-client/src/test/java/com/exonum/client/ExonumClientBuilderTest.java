package com.exonum.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ExonumClientBuilderTest {

  @Test
  void requiredFieldsWereNotSet() {
    assertThrows(IllegalStateException.class, () -> ExonumClient.newBuilder().build());
  }

  @Test
  void invalidUrl() {
    assertThrows(IllegalArgumentException.class,
        () -> ExonumClient.newBuilder().setExonumHost("invalid-url").build());
  }

}
