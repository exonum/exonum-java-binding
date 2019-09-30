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

import static com.exonum.client.ExonumApi.JSON;

import com.exonum.client.response.ConsensusStatus;
import com.exonum.client.response.HealthCheckInfo;
import com.exonum.client.response.SystemStatistics;
import lombok.Value;

/**
 * Utility class for Exonum System API.
 */
final class SystemApiHelper {

  static HealthCheckInfo parseHealthCheckJson(String json) {
    HealthCheckResponse response = JSON.fromJson(json, HealthCheckResponse.class);
    String consensusStatus = response.getConsensusStatus().toUpperCase();

    return new HealthCheckInfo(
        ConsensusStatus.valueOf(consensusStatus),
        response.getConnectedPeers()
    );
  }

  static SystemStatistics parseStatsJson(String json) {
    StatsResponse response = JSON.fromJson(json, StatsResponse.class);
    return new SystemStatistics(response.txPoolSize, response.txCount);
  }

  /**
   * Json object wrapper for memory pool response.
   */
  @Value
  private static class StatsResponse {
    int txPoolSize;
    long txCount;
  }

  /**
   * Json object wrapper for health check response.
   */
  @Value
  private static class HealthCheckResponse {
    String consensusStatus;
    int connectedPeers;
  }

  private SystemApiHelper() {
    throw new UnsupportedOperationException("Not instantiable");
  }
}
