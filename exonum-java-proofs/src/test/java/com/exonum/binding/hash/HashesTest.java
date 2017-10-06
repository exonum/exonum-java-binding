package com.exonum.binding.hash;

import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.UnsignedBytes;
import java.nio.ByteBuffer;
import org.junit.Test;

public class HashesTest {

  private static final byte[] ZERO_HASH = zeroSha256Hash();

  @Test
  public void getHashOfMultipleElements() throws Exception {
    byte[] e1 = bytes("first");
    byte[] e2 = bytes("second");
    byte[] e3 = bytes("third");


    byte[] all = Bytes.concat(e1, e2, e3);
    assertThat(Hashes.getHashOf(e1, e2, e3), equalTo(Hashes.getHashOf(all)));
  }

  @Test
  public void getHashOfSecondEmptyElement() throws Exception {
    byte[] e1 = bytes("first");
    byte[] e2 = bytes();


    byte[] all = Bytes.concat(e1, e2);
    assertThat(Hashes.getHashOf(e1, e2), equalTo(Hashes.getHashOf(all)));
  }

  @Test
  public void getHashOfEmptyArray() throws Exception {
    assertThat(Hashes.getHashOf(bytes()), equalTo(ZERO_HASH));
  }

  @Test
  public void getHashOfNoArgs() throws Exception {
    assertThat(Hashes.getHashOf(), equalTo(ZERO_HASH));
  }

  @Test
  public void getHashOfEmptyByteBuffer() throws Exception {
    assertThat(Hashes.getHashOf(ByteBuffer.allocate(0)), equalTo(ZERO_HASH));
  }

  @Test
  public void getHashOfNonEmptyByteBuffer() throws Exception {
    byte[] inputBytes = bytes("some input bytes");
    ByteBuffer inputBuffer = ByteBuffer.wrap(inputBytes);
    byte[] expectedHash = Hashes.getHashOf(inputBytes);

    assertThat(Hashes.getHashOf(inputBuffer), equalTo(expectedHash));
  }

  private static byte[] zeroSha256Hash() {
    String zeroHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    int hashSize = Hashes.HASH_SIZE_BYTES;
    byte[] zeroHashBytes = new byte[hashSize];
    for (int i = 0; i < hashSize; i++) {
      int startIndex = i * 2;
      zeroHashBytes[i] = UnsignedBytes.parseUnsignedByte(
          zeroHash.substring(startIndex, startIndex + 2), 16);
    }
    return zeroHashBytes;
  }

  @Test
  public void toStringAllHexNumbersLower() throws Exception {
    for (byte b = 0; b <= 0xF; b++) {
      String expected = "0" + UnsignedBytes.toString(b, 16);
      assertThat(Hashes.toString(bytes(b)), equalTo(expected));
    }
  }

  @Test
  public void toStringAllHexNumbersUpper() throws Exception {
    for (int i = 1; i <= 0xF; i++) {
      byte b = (byte) (i << 4);
      String expected = UnsignedBytes.toString(b, 16);
      assertThat(Hashes.toString(bytes(b)), equalTo(expected));
    }
  }
}
