/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.qaservice.transactions;

import com.exonum.binding.qaservice.PromoteToCore;
import com.exonum.binding.qaservice.QaService;

/**
 * Parameters of any transaction. Simplifies converting them from binary to JSON.
 *
 * @param <BodyT> type of the transaction message body (aka payload)
 */
@PromoteToCore("A similar class might be universally useful if we need to serialize transaction "
    + "data into JSON in the Exonum standard format.")
public final class AnyTransaction<BodyT> {
  public final short service_id;
  public final short message_id;
  public final BodyT body;

  /** Creates a new transaction message. */
  public AnyTransaction(short message_id,
                        BodyT body) {
    this(QaService.ID, message_id, body);
  }

  /** Creates a new transaction message. */
  private AnyTransaction(short service_id,
                         short message_id,
                         BodyT body) {
    this.service_id = service_id;
    this.message_id = message_id;
    this.body = body;
  }
}
