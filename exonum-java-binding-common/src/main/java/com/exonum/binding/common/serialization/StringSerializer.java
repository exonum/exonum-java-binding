/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.exonum.binding.common.serialization;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

enum StringSerializer implements Serializer<String> {
  INSTANCE;

  @Override
  public byte[] toBytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public String fromBytes(byte[] serializedValue) {
    try {
      // Since the String(bytes, charset) constructor is specified so that
      // it "… always replaces malformed-input and unmappable-character sequences …",
      // it is not suitable for our use-case: we must reject malformed input.

      // Create a new decoder
      CharsetDecoder decoder = StandardCharsets.UTF_8
          .newDecoder()
          // Reject (= report as exception) malformed input.
          .onMalformedInput(CodingErrorAction.REPORT)
          // In case some valid UTF-8 characters are not encodable in UTF-16,
          // we replace them with the default replacement character.
          .onUnmappableCharacter(CodingErrorAction.REPLACE);

      // Decode the buffer in a character buffer
      CharBuffer strBuffer = decoder.decode(ByteBuffer.wrap(serializedValue));
      return new String(strBuffer.array(), strBuffer.arrayOffset(), strBuffer.remaining());
    } catch (CharacterCodingException e) {
      throw new IllegalArgumentException("Cannot decode the input", e);
    }
  }
}
