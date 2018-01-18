package com.exonum.binding.service;

import com.exonum.binding.service.adapters.UserServiceAdapter;
import com.exonum.binding.transport.Server;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * A framework module which configures the system-wide bindings.
 */
class FrameworkModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Server.class).in(Singleton.class);
    //  fixme: if that's a proxy of a NodeProxy :-)
    // bind(Node.class);
    bind(UserServiceAdapter.class);
  }
}
