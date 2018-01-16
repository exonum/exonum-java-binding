package com.exonum.binding.fakes.services;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.exonum.binding.service.adapters.UserServiceAdapter;
import org.junit.Test;

public class UserServiceAdapterMockBuilderTest {

  @Test
  public void buildWithId() {
    short id = 10;
    UserServiceAdapterMockBuilder builder = new UserServiceAdapterMockBuilder();
    builder.id(id);
    UserServiceAdapter service = builder.build();

    assertThat(service.getId(), equalTo(id));
  }
}
