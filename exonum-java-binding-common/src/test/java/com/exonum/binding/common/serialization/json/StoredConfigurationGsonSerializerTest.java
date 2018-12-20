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

import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.exonum.binding.common.configuration.ConsensusConfiguration;
import com.exonum.binding.common.configuration.StoredConfiguration;
import com.exonum.binding.common.configuration.ValidatorKey;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import java.util.List;
import org.junit.jupiter.api.Test;

class StoredConfigurationGsonSerializerTest {

  private static final String CONFIG_EXAMPLE = "{\n"
      + "\"previous_cfg_hash\": \"000000000000000000000000000000000000000000000000000000000000000"
      + "0\",\n"
      + "\"actual_from\": 0,\n"
      + "\"validator_keys\": [\n"
      + "    {\n"
      + "        \"consensus_key\": \"43eb3be553c55b02b65e08c18bb060404b27e362ccf108cbad94ea097de"
      + "cbc0a\",\n"
      + "        \"service_key\": \"79c1fcefcbfaeae43575ab0ef793c24aae7b39186244e6552c18b8f7d0b0d"
      + "e12\"\n"
      + "    }\n"
      + "],\n"
      + "\"consensus\": {\n"
      + "    \"round_timeout\": 3000,\n"
      + "    \"status_timeout\": 5000,\n"
      + "    \"peers_timeout\": 10000,\n"
      + "    \"txs_block_limit\": 1000,\n"
      + "    \"max_message_len\": 1048576,\n"
      + "    \"min_propose_timeout\": 10,\n"
      + "    \"max_propose_timeout\": 200,\n"
      + "    \"propose_timeout_threshold\": 500\n"
      + "},\n"
      + "\"majority_count\": null,\n"
      + "\"services\": {\n"
      + "    \"configuration\": null,\n"
      + "    \"cryptocurrency-demo-service\": null\n"
      + "}\n"
      + "}";

  @Test
  void roundTripTest() {
    StoredConfiguration configuration = createConfiguration();
    StoredConfiguration restoredConfiguration = json().fromJson(
        json().toJson(configuration), StoredConfiguration.class);

    assertThat(restoredConfiguration, equalTo(configuration));
  }

  @Test
  void readConfiguration() {
    StoredConfiguration configuration = json()
        .fromJson(CONFIG_EXAMPLE, StoredConfiguration.class);

    assertThat(configuration, notNullValue());
    assertThat(configuration.previousCfgHash(),
        is(HashCode
            .fromString("0000000000000000000000000000000000000000000000000000000000000000"))
    );
    assertThat(configuration.actualFrom(), is(0L));

    ConsensusConfiguration consensusConfiguration = configuration.consensusConfiguration();
    assertThat(consensusConfiguration, notNullValue());
    assertThat(consensusConfiguration.maxMessageLen(), is(1048576));
    assertThat(consensusConfiguration.maxProposeTimeout(), is(200L));
    assertThat(consensusConfiguration.minProposeTimeout(), is(10L));
    assertThat(consensusConfiguration.peersTimeout(), is(10000L));
    assertThat(consensusConfiguration.proposeTimeoutThreshold(), is(500));
    assertThat(consensusConfiguration.roundTimeout(), is(3000L));
    assertThat(consensusConfiguration.statusTimeout(), is(5000L));
    assertThat(consensusConfiguration.txsBlockLimit(), is(1000));

    List<ValidatorKey> expectedValidatorKeys = singletonList(ValidatorKey.builder()
        .consensusKey(PublicKey.fromHexString(
            "43eb3be553c55b02b65e08c18bb060404b27e362ccf108cbad94ea097decbc0a"))
        .serviceKey(PublicKey.fromHexString(
            "79c1fcefcbfaeae43575ab0ef793c24aae7b39186244e6552c18b8f7d0b0de12"))
        .build());
    assertThat(configuration.validatorKeys(), is(expectedValidatorKeys));
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
