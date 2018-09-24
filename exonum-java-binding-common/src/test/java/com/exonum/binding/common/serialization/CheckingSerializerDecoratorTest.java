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

package com.exonum.binding.common.serialization;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

@SuppressWarnings("unchecked") // No type parameters for clarity
class CheckingSerializerDecoratorTest {

  private Serializer delegateMock;

  private CheckingSerializerDecorator decorator;

  @BeforeEach
  void setUp() {
    delegateMock = mock(Serializer.class);
    decorator = CheckingSerializerDecorator.from(delegateMock);
  }

  @Test
  void fromSelf() {
    assertThat(CheckingSerializerDecorator.from(decorator), sameInstance(decorator));
  }

  @Test
  void toBytes() {
    Object value = new Object();
    byte[] valueBytes = new byte[0];
    when(delegateMock.toBytes(value)).thenReturn(valueBytes);

    assertThat(decorator.toBytes(value), equalTo(valueBytes));
  }

  @Test
  void toBytes_NullValue() {
    assertThrows(NullPointerException.class, () -> decorator.toBytes(null));
  }

  @Test
  void toBytes_NullFromDelegate() {
    when(delegateMock.toBytes(any())).thenReturn(null);

    expectBrokenSerializerException(() -> decorator.toBytes(new Object()));
  }

  @Test
  void fromBytes() {
    Object value = new Object();
    byte[] valueBytes = new byte[0];
    when(delegateMock.fromBytes(valueBytes)).thenReturn(value);

    assertThat(decorator.fromBytes(valueBytes), equalTo(value));
  }

  @Test
  void fromBytes_NullValue() {
    assertThrows(NullPointerException.class, () -> decorator.toBytes(null));
  }

  @Test
  void fromBytes_NullFromDelegate() {
    when(delegateMock.fromBytes(any())).thenReturn(null);

    expectBrokenSerializerException(() -> decorator.fromBytes(new byte[0]));
  }

  private void expectBrokenSerializerException(Executable function) {
    IllegalStateException thrown = assertThrows(IllegalStateException.class, function);
    assertThat(thrown.getMessage(), containsString("Broken serializer"));
  }
}
