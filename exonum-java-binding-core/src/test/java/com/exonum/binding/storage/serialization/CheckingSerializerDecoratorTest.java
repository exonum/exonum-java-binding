package com.exonum.binding.storage.serialization;

import static org.hamcrest.CoreMatchers.equalTo;
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
    decorator = new CheckingSerializerDecorator(delegateMock);
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

    expectNpe("Broken serializer");
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

    expectNpe("Broken serializer");
    decorator.fromBytes(new byte[0]);
  }

  private void expectNpe(String messageSubstring) {
    expectedException.expectMessage(messageSubstring);
    expectedException.expect(NullPointerException.class);
  }

  private void expectNpe() {
    expectedException.expect(NullPointerException.class);
  }
}
