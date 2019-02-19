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
 *
 */

package com.exonum.client;

import static com.exonum.binding.common.serialization.json.JsonSerializer.json;

import com.exonum.client.response.ConsensusStatus;
import com.exonum.client.response.HealthCheckInfo;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Value;

/**
 * Utility class for Exonum System API.
 */
final class SystemApiHelper {

  static HealthCheckInfo healthCheckJsonParser(String json) {
    HealthCheckResponse response = json().fromJson(json, HealthCheckResponse.class);
    String consensusStatus = response.getConsensusStatus().toUpperCase();
    JsonElement connectivity = response.getConnectivity();
    if (connectivity.isJsonObject()) {
      JsonObject connectivityObject = connectivity.getAsJsonObject();
      int connectionsNumber = connectivityObject
          .get("Connected").getAsJsonObject()
          .get("amount").getAsInt();
      return new HealthCheckInfo(ConsensusStatus.valueOf(consensusStatus), connectionsNumber);
    } else {
      return new HealthCheckInfo(ConsensusStatus.valueOf(consensusStatus), 0);
    }
  }

  static int memoryPoolJsonParser(String json) {
    MemoryPoolResponse response = json().fromJson(json, MemoryPoolResponse.class);
    return response.getSize();
  }

  /**
   * Json object wrapper for memory pool response.
   */
  @Value
  private static class MemoryPoolResponse {
    int size;
  }

  /**
   * Json object wrapper for health check response.
   */
  @Value
  private class HealthCheckResponse {
    @SerializedName("consensus_status")
    String consensusStatus;
    JsonElement connectivity;
  }

  private SystemApiHelper() {
    throw new UnsupportedOperationException("Not instantiable");
  }
}
