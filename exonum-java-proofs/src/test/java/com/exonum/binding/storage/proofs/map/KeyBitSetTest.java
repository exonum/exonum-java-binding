package com.exonum.binding.storage.proofs.map;

import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class KeyBitSetTest {

  @Test
  public void commonPrefixTest() {
    KeyBitSet firstKey = new KeyBitSet(bytes(0xB), 4);
    KeyBitSet secondKey = new KeyBitSet(bytes(0xF), 4);
    KeyBitSet expectedCommonPrefixKey = new KeyBitSet(bytes(0x3), 2);
    KeyBitSet actualCommonPrefixKey = firstKey.commonPrefix(secondKey);
    assertThat(actualCommonPrefixKey, equalTo(expectedCommonPrefixKey));
  }
}
