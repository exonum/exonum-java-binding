package com.exonum.binding.cryptocurrency.transactions;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.Map;
import org.junit.Test;

public class TransactionJsonMessageTest {

  @Test
  public void fromJson() {
    String message = "{ "
        + "\"protocol_version\": 1, "
        + "\"service_id\": 2, "
        + "\"message_id\": 3, "
        + "\"body\": {"
        + "  \"ownerPublicKey\": \"ab\""
        + "}, "
        + "\"signature\": \"cd\""
        + " }";
    Gson gson = CryptocurrencyTransactionGson.instance();

    TransactionJsonMessage tx = gson.fromJson(message,
        new TypeToken<TransactionJsonMessage<Map<String, String>>>() {}.getType());

    assertThat(tx.getProtocolVersion()).isEqualTo((byte) 1);
    assertThat(tx.getServiceId()).isEqualTo((short) 2);
    assertThat(tx.getMessageId()).isEqualTo((short) 3);
    assertThat(tx.getBody()).isEqualTo(ImmutableMap.of("ownerPublicKey", "ab"));
    assertThat(tx.getSignature()).isEqualTo("cd");
  }
}
