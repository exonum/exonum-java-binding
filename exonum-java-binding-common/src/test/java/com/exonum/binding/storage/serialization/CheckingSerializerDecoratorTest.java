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

package com.exonum.binding.storage.serialization;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SuppressWarnings("unchecked") // No type parameters for clarity
public class CheckingSerializerDecoratorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  Serializer delegateMock;

  CheckingSerializerDecorator decorator;

  @Before
  public void setUp() {
    delegateMock = mock(Serializer.class);
    decorator = CheckingSerializerDecorator.from(delegateMock);
  }

  @Test
  public void fromSelf() {
    assertThat(CheckingSerializerDecorator.from(decorator), sameInstance(decorator));
  }

  @Test
  public void toBytes() {
    Object value = new Object();
    byte[] valueBytes = new byte[0];
    when(delegateMock.toBytes(value)).thenReturn(valueBytes);

    assertThat(decorator.toBytes(value), equalTo(valueBytes));
  }

  @Test
  public void toBytes_NullValue() {
    expectNpe();
    decorator.toBytes(null);
  }

  @Test
  public void toBytes_NullFromDelegate() {
    when(delegateMock.toBytes(any())).thenReturn(null);

    expectBrokenSerializerException();
    decorator.toBytes(new Object());
  }

  @Test
  public void fromBytes() {
    Object value = new Object();
    byte[] valueBytes = new byte[0];
    when(delegateMock.fromBytes(valueBytes)).thenReturn(value);

    assertThat(decorator.fromBytes(valueBytes), equalTo(value));
  }

  @Test
  public void fromBytes_NullValue() {
    expectNpe();
    decorator.fromBytes(null);
  }

  @Test
  public void fromBytes_NullFromDelegate() {
    when(delegateMock.fromBytes(any())).thenReturn(null);

    expectBrokenSerializerException();
    decorator.fromBytes(new byte[0]);
  }

  private void expectBrokenSerializerException() {
    expectedException.expectMessage("Broken serializer");
    expectedException.expect(IllegalStateException.class);
  }

  private void expectNpe() {
    expectedException.expect(NullPointerException.class);
  }
}
