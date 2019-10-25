/*
 * Copyright 2019 The Exonum Team
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
 */

package com.exonum.binding.testkit;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.core.transaction.TransactionContext;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public final class TestTransaction implements Transaction {

  static final short ID = 94;
  static final Charset BODY_CHARSET = StandardCharsets.UTF_8;

  private final String value;

  static TestTransaction from(int txId, byte[] arguments) {
    checkArgument(txId == ID);
    String value = getValue(arguments);
    return new TestTransaction(value);
  }

  private static String getValue(byte[] payload) {
    try {
      CharsetDecoder utf8Decoder = createUtf8Decoder();
      ByteBuffer body = ByteBuffer.wrap(payload);
      CharBuffer result = utf8Decoder.decode(body);
      return result.toString();
    } catch (CharacterCodingException e) {
      throw new IllegalArgumentException("Cannot decode the message body", e);
    }
  }

  private static CharsetDecoder createUtf8Decoder() {
    return BODY_CHARSET.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPLACE);
  }

  private TestTransaction(String value) {
    this.value = value;
  }

  @Override
  public void execute(TransactionContext context) {
    // TODO: put serviceId and serviceName into TransactionContext and use it when
    //  creating TestSchema [ECR-3639]
    TestSchema schema = new TestSchema(context.getFork(), 46);
    ProofMapIndexProxy<HashCode, String> map = schema.testMap();
    map.put(getKey(), value);
  }

  private HashCode getKey() {
    return Hashing.defaultHashFunction()
        .hashString(value, BODY_CHARSET);
  }
}
