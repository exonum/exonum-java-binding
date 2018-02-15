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
  public void setUp() throws Exception {
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
