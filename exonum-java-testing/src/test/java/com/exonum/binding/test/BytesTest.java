package com.exonum.binding.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.primitives.UnsignedBytes;
import org.junit.Test;

public class BytesTest {

  @Test
  public void fromHex() {
    assertThat(Bytes.fromHex("abcd01"), equalTo(new byte[] {UnsignedBytes.checkedCast(0xAB),
        UnsignedBytes.checkedCast(0xCD), 0x01}));
  }
}
