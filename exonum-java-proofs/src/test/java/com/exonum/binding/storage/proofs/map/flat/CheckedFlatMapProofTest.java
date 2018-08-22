package com.exonum.binding.storage.proofs.map.flat;

import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class CheckedFlatMapProofTest {

  @Test
  public void determineAbsenceOfMissingKeys() {
    byte[] firstFoundKey = bytes(0x01);
    byte[] secondFoundKey = bytes(0x02);
    List<byte[]> requestedKeys = new ArrayList<>(Arrays.asList(firstFoundKey, secondFoundKey));
    List<byte[]> foundKeys = new ArrayList<>(Arrays.asList(firstFoundKey, secondFoundKey));
    List<byte[]> expectedMissingKeys = Collections.emptyList();
    List<byte[]> actualMissingKeys =
        CheckedFlatMapProof.determineMissingKeys(requestedKeys, foundKeys);
    assertThat(expectedMissingKeys, containsInAnyOrder(actualMissingKeys.toArray()));
  }

  @Test
  public void determineMissingKeysWithTwoMissingKeys() {
    byte[] foundKey = bytes(0x01);
    byte[] firstMissingKey = bytes(0x02);
    byte[] secondMissingKey = bytes(0x03);
    List<byte[]> requestedKeys =
        new ArrayList<>(Arrays.asList(foundKey, firstMissingKey, secondMissingKey));
    List<byte[]> foundKeys = Collections.singletonList(foundKey);
    List<byte[]> expectedMissingKeys =
        new ArrayList<>(Arrays.asList(firstMissingKey, secondMissingKey));
    List<byte[]> actualMissingKeys =
        CheckedFlatMapProof.determineMissingKeys(requestedKeys, foundKeys);
    assertThat(expectedMissingKeys, containsInAnyOrder(actualMissingKeys.toArray()));
  }
}
