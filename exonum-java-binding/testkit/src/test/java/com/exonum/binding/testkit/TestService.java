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

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.service.AbstractService;
import com.exonum.binding.core.service.BlockCommittedEvent;
import com.exonum.binding.core.service.Configuration;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.storage.database.Access;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.core.transaction.ExecutionException;
import com.exonum.binding.core.transaction.RawTransaction;
import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.testkit.TestProtoMessages.TestConfiguration;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public final class TestService extends AbstractService {

  static final HashCode INITIAL_ENTRY_KEY =
      Hashing.defaultHashFunction().hashString("Initial key", StandardCharsets.UTF_8);
  static final String THROWING_VALUE = "Incorrect value";
  static final byte ANY_ERROR_CODE = 127;
  static final short TEST_TRANSACTION_ID = 94;
  static final Charset BODY_CHARSET = StandardCharsets.UTF_8;

  private Node node;

  @Inject
  public TestService(ServiceInstanceSpec serviceSpec) {
    super(serviceSpec);
  }

  @Override
  protected TestSchema createDataSchema(Access access) {
    return new TestSchema(access, getId());
  }

  @Override
  public void initialize(Fork fork, Configuration configuration) {
    TestConfiguration initialConfiguration = configuration.getAsMessage(TestConfiguration.class);
    String configurationValue = initialConfiguration.getValue();
    if (configurationValue.equals(THROWING_VALUE)) {
      throw new ExecutionException(
          ANY_ERROR_CODE, "Service configuration had an invalid value: " + configurationValue);
    }
    TestSchema schema = createDataSchema(fork);
    ProofMapIndexProxy<HashCode, String> testMap = schema.testMap();
    testMap.put(INITIAL_ENTRY_KEY, configurationValue);
  }

  @Transaction(TEST_TRANSACTION_ID)
  public void putEntry(byte[] arguments, TransactionContext context) {
    String value = getValue(arguments);

    TestSchema schema = new TestSchema(context.getFork(), context.getServiceId());
    ProofMapIndexProxy<HashCode, String> map = schema.testMap();
    map.put(getKey(value), value);
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
    return BODY_CHARSET
        .newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPLACE);
  }

  private HashCode getKey(String value) {
    return Hashing.defaultHashFunction().hashString(value, BODY_CHARSET);
  }

  @Override
  public void afterCommit(BlockCommittedEvent event) {
    long height = event.getHeight();
    RawTransaction rawTransaction = constructAfterCommitTransaction(getId(), height);
    node.submitTransaction(rawTransaction);
  }

  static RawTransaction constructAfterCommitTransaction(int serviceId, long height) {
    String payload = "Test message on height " + height;
    return RawTransaction.newBuilder()
        .serviceId(serviceId)
        .transactionId(TEST_TRANSACTION_ID)
        .payload(payload.getBytes(BODY_CHARSET))
        .build();
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    this.node = node;
  }
}
