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

package com.exonum.binding.fakes.mocks;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.messages.Message;
import com.exonum.binding.service.adapters.UserServiceAdapter;
import org.junit.jupiter.api.Test;

class UserServiceAdapterMockBuilderTest {

  private static final int MIN_MESSAGE_SIZE = Message.messageSize(0);

  @Test
  void buildWithId() {
    short id = 10;
    UserServiceAdapterMockBuilder builder = new UserServiceAdapterMockBuilder();
    builder.id(id);
    UserServiceAdapter service = builder.build();

    assertThat(service.getId(), equalTo(id));
  }

  @Test
  void buildThrowing() {
    UserServiceAdapterMockBuilder builder = new UserServiceAdapterMockBuilder();
    Class<? extends Throwable> exceptionType = IllegalArgumentException.class;
    builder.convertTransactionThrowing(exceptionType);
    UserServiceAdapter service = builder.build();

    byte[] rawTxMessage = new byte[MIN_MESSAGE_SIZE];
    assertThrows(exceptionType, () -> service.convertTransaction(rawTxMessage));
  }

  @Test
  void buildWithNullConfig() {
    UserServiceAdapterMockBuilder builder = new UserServiceAdapterMockBuilder();
    builder.initialGlobalConfig(null);

    UserServiceAdapter service = builder.build();

    assertNull(service.initialize(10L));
  }
}
