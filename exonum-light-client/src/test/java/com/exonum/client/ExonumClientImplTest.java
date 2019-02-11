package com.exonum.client;

import static com.exonum.client.ExonumUrls.SUBMIT_TRANSACTION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExonumClientImplTest {

  private static final MockWebServer SERVER = new MockWebServer();

  private ExonumClient exonumClient;

  @BeforeAll
  static void start() throws IOException {
    SERVER.start();
  }

  @BeforeEach
  void setup() {
    exonumClient = ExonumClient.newBuilder()
        .setExonumHost(SERVER.url("/").url())
        .build();
  }

  @Test
  void submitTransaction() throws InterruptedException {
    String hash = "f128c720e04b8243";
    String body = "{\"tx_hash\":\"" + hash + "\"}";
    SERVER.enqueue(new MockResponse().setBody(body));

    TransactionMessage txMessage = mock(TransactionMessage.class);
    when(txMessage.toBytes()).thenReturn(new byte[]{0x00});

    HashCode hashCode = exonumClient.submitTransaction(txMessage);
    assertThat(hashCode, is(HashCode.fromString(hash)));

    RecordedRequest recordedRequest = SERVER.takeRequest();
    assertThat(recordedRequest.getMethod(), is("POST"));
    assertThat(recordedRequest.getPath(), is(SUBMIT_TRANSACTION));
  }

  @AfterAll
  static void shutdown() throws IOException {
    SERVER.shutdown();
  }

}
