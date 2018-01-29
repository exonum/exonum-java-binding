package com.exonum.binding.fakes.services;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.exonum.binding.service.adapters.UserServiceAdapter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class UserServiceAdapterMockBuilderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void buildWithId() {
    short id = 10;
    UserServiceAdapterMockBuilder builder = new UserServiceAdapterMockBuilder();
    builder.id(id);
    UserServiceAdapter service = builder.build();

    assertThat(service.getId(), equalTo(id));
  }

  @Test
  public void buildThrowing() {
    UserServiceAdapterMockBuilder builder = new UserServiceAdapterMockBuilder();
    builder.rejectingRawTransactions();
    UserServiceAdapter service = builder.build();

    byte[] rawTxMessage = new byte[64];
    expectedException.expect(IllegalArgumentException.class);
    service.convertTransaction(rawTxMessage);
  }
}
