package com.exonum.binding.storage.proofs.map.flat;

import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class CheckedFlatMapProofTest {

  @Test
  public void determineAbsenceOfMissingKeys() {
    byte[] firstFoundKey = bytes(0x01);
    byte[] secondFoundKey = bytes(0x02);
    Set<byte[]> requestedKeys = new HashSet<>(Arrays.asList(firstFoundKey, secondFoundKey));
    Set<byte[]> foundKeys = new HashSet<>(Arrays.asList(firstFoundKey, secondFoundKey));
    Set<byte[]> expectedMissingKeys = Collections.emptySet();
    Set<byte[]> actualMissingKeys =
        CheckedFlatMapProof.determineMissingKeys(requestedKeys, foundKeys);
    assertThat(expectedMissingKeys, containsInAnyOrder(actualMissingKeys.toArray()));
  }

  @Test
  public void determineMissingKeysWithTwoMissingKeys() {
    byte[] foundKey = bytes(0x01);
    byte[] firstMissingKey = bytes(0x02);
    byte[] secondMissingKey = bytes(0x03);
    Set<byte[]> requestedKeys =
        new HashSet<>(Arrays.asList(foundKey, firstMissingKey, secondMissingKey));
    Set<byte[]> foundKeys = Collections.singleton(foundKey);
    Set<byte[]> expectedMissingKeys =
        new HashSet<>(Arrays.asList(firstMissingKey, secondMissingKey));
    Set<byte[]> actualMissingKeys =
        CheckedFlatMapProof.determineMissingKeys(requestedKeys, foundKeys);
    assertThat(expectedMissingKeys, containsInAnyOrder(actualMissingKeys.toArray()));
  }
}
