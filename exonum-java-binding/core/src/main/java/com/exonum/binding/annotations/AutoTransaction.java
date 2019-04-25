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

package com.exonum.binding.annotations;

import com.exonum.binding.common.message.TransactionMessage;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates thе factory method to be used to decode the transaction message,
 * that has the specified {@linkplain TransactionMessage#getTransactionId() transaction identifier}.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({
    ElementType.METHOD,
})
public @interface AutoTransaction {

  /**
   * Specifies the {@linkplain TransactionMessage#getTransactionId() transaction type identifier}.
   * Must be unique within a service.
   */
  short value();

  // todo: enable
  //  /**
  //   * Specifies the {@linkplain TransactionMessage#getTransactionId() transaction type
  //   * identifier}.
  //   * Must be unique within a service.
  //   */
  //  short id();
}
