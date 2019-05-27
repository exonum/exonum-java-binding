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

package com.exonum.client.response;

import lombok.Value;

@Value
public class HealthCheckInfo {
  /**
   * Consensus status shows/indicates whether it is possible
   * to achieve the consensus between validators in the current network state.
   */
  ConsensusStatus consensusStatus;

  /**
   * The number of peers that the node is connected to;
   * {@code = 0} if the node is not connected to the network,
   * or it's the single node network.
   */
  int connectionsNumber;
}
