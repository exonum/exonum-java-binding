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

package com.exonum.binding.cryptocurrency.transactions;

/** Base transaction with common fields, used by all transaction types. */
public class BaseTx {
  protected final short service_id;
  protected final short message_id;

  public BaseTx(short service_id, short message_id) {
    this.service_id = service_id;
    this.message_id = message_id;
  }

  public short getServiceId() {
    return service_id;
  }

  public short getMessageId() {
    return message_id;
  }
}
