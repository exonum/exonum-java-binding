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
 */

package com.exonum.binding.common.serialization.json;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.configuration.ConsensusConfiguration;
import com.exonum.binding.common.configuration.StoredConfiguration;
import com.exonum.binding.common.configuration.ValidatorKey;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import org.junit.jupiter.api.Test;

class StoredConfigurationGsonSerializerTest {

  private static final String CONFIG_EXAMPLE = "{\t\n"
      + "\"previous_cfg_hash\": \"0000000000000000000000000000000000000000000000000000000000000000"
      + "\",\t\n"
      + "\"actual_from\": 0,\t\n"
      + "\"validator_keys\": [\t\n"
      + "    {\t\n"
      + "        \"consensus_key\": \"43eb3be553c55b02b65e08c18bb060404b27e362ccf108cbad94ea09"
      + "7decbc0a\",\t\n"
      + "        \"service_key\": \"79c1fcefcbfaeae43575ab0ef793c24aae7b39186244e6552c18b8f7d0"
      + "b0de12\"\t\n"
      + "    }\t\n"
      + "],\t\n"
      + "\"consensus\": {\t\n"
      + "    \"round_timeout\": 3000,\t\n"
      + "    \"status_timeout\": 5000,\t\n"
      + "    \"peers_timeout\": 10000,\t\n"
      + "    \"txs_block_limit\": 1000,\t\n"
      + "    \"max_message_len\": 1048576,\t\n"
      + "    \"min_propose_timeout\": 10,\t\n"
      + "    \"max_propose_timeout\": 200,\t\n"
      + "    \"propose_timeout_threshold\": 500\t\n"
      + "},\t\n"
      + "\"majority_count\": null,\t\n"
      + "\"services\": {\t\n"
      + "    \"configuration\": null,\t\n"
      + "    \"cryptocurrency-demo-service\": null\t\n"
      + "}\t\n"
      + "}";

  @Test
  void roundTripTest() {
    StoredConfiguration configuration = createConfiguration();
    StoredConfiguration restoredConfiguration = StoredConfigurationGsonSerializer
        .fromJson(StoredConfigurationGsonSerializer.toJson(configuration));

    assertThat(restoredConfiguration, equalTo(configuration));
  }

  @Test
  void nullPointerException() {
    StoredConfiguration configuration = null;
    String jsonConfiguration = null;

    assertThrows(NullPointerException.class,
        () -> StoredConfigurationGsonSerializer.toJson(configuration));

    assertThrows(NullPointerException.class,
        () -> StoredConfigurationGsonSerializer.fromJson(jsonConfiguration));
  }

  @Test
  void readConfiguration() {
    StoredConfiguration configuration = StoredConfigurationGsonSerializer
        .fromJson(CONFIG_EXAMPLE);

    assertThat(configuration, notNullValue());
    assertThat(configuration.previousCfgHash(),
        is(HashCode
            .fromString("0000000000000000000000000000000000000000000000000000000000000000"))
    );
    assertThat(configuration.actualFrom(), is(0L));

    assertThat(configuration.consensusConfiguration(), notNullValue());
    assertThat(configuration.consensusConfiguration().maxMessageLen(), is(1048576));
    assertThat(configuration.consensusConfiguration().maxProposeTimeout(), is(200L));
    assertThat(configuration.consensusConfiguration().minProposeTimeout(), is(10L));
    assertThat(configuration.consensusConfiguration().peersTimeout(), is(10000L));
    assertThat(configuration.consensusConfiguration().proposeTimeoutThreshold(), is(500));
    assertThat(configuration.consensusConfiguration().roundTimeout(), is(3000L));
    assertThat(configuration.consensusConfiguration().statusTimeout(), is(5000L));
    assertThat(configuration.consensusConfiguration().txsBlockLimit(), is(1000));

    assertThat(configuration.validatorKeys(), notNullValue());
    assertThat(configuration.validatorKeys().get(0).consensusKey(), is(PublicKey.fromHexString(
        "43eb3be553c55b02b65e08c18bb060404b27e362ccf108cbad94ea097decbc0a")));
    assertThat(configuration.validatorKeys().get(0).serviceKey(), is(PublicKey.fromHexString(
        "79c1fcefcbfaeae43575ab0ef793c24aae7b39186244e6552c18b8f7d0b0de12")));
  }

  private StoredConfiguration createConfiguration() {
    return StoredConfiguration.builder()
        .previousCfgHash(HashCode.fromString("11"))
        .actualFrom(1)
        .validatorKeys(
            singletonList(
                ValidatorKey.builder()
                    .consensusKey(PublicKey.fromHexString("22"))
                    .serviceKey(PublicKey.fromHexString("33"))
                    .build()
            )
        )
        .consensusConfiguration(
            ConsensusConfiguration.builder()
                .roundTimeout(1)
                .statusTimeout(2)
                .peersTimeout(3)
                .txsBlockLimit(4)
                .maxMessageLen(5)
                .minProposeTimeout(6)
                .maxProposeTimeout(7)
                .proposeTimeoutThreshold(8)
                .build()
        )
        .build();
  }
}
