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

package com.exonum.client.response;

import lombok.Value;

// todo: I don't like that this class is vaguely defined (just as the endpoint) as SystemStatistics,
//    because it is unclear what kind of things a user can find inside unless they look here.
//    A related consideration is if we shall keep ExonumClient#getNumUnconfirmedTransactions
//    or replace it with ExonumClient#getSystemStats — which does not tell what kind of stats
//    a user can find there, but allows to add new statistics if needed without increasing the
//    direct size of ExonumClient API.
@Value
public class SystemStatistics {
  /**
   * A number of {@linkplain TransactionStatus#IN_POOL unconfirmed} transactions which are currently
   * located in the unconfirmed transactions pool and are waiting for acceptance in a block.
   */
  int numUnconfirmedTransactions;

  /**
   * A number of {@linkplain TransactionStatus#COMMITTED committed} transactions in the blockchain.
   */
  long numCommittedTransactions;
}