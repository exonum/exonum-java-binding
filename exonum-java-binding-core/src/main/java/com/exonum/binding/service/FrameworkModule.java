package com.exonum.binding.service;

import static com.google.inject.matcher.Matchers.any;
import static com.google.inject.matcher.Matchers.subclassesOf;

import com.exonum.binding.service.adapters.UserServiceAdapter;
import com.exonum.binding.service.adapters.ViewFactory;
import com.exonum.binding.service.adapters.ViewProxyFactory;
import com.exonum.binding.transport.Server;
import com.exonum.binding.util.LoggingInterceptor;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * A framework module which configures the system-wide bindings.
 */
class FrameworkModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Server.class).toProvider(Server::create).in(Singleton.class);
    bind(ViewFactory.class).toInstance(ViewProxyFactory.getInstance());
    //  fixme: if that's a proxy of a NodeProxy :-)
    // bind(Node.class);
    bindInterceptor(subclassesOf(UserServiceAdapter.class), any(), new LoggingInterceptor());
    bind(UserServiceAdapter.class);
  }
}
