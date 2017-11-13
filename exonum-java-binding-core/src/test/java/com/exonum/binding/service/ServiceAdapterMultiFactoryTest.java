package com.exonum.binding.service;

import org.junit.Test;

public class ServiceAdapterMultiFactoryTest {

  @Test
  public void createService() throws Exception {
    ServiceAdapterMultiFactory.getInstance().createService();
  }

}
