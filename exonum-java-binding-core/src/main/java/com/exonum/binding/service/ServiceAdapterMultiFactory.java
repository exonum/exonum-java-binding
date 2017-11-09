package com.exonum.binding.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.service.spi.ServiceFactory;
import com.exonum.binding.transport.Server;
import java.util.Iterator;
import java.util.ServiceLoader;

public enum ServiceAdapterMultiFactory {
  INSTANCE;

  private final Server server;
  private final ServiceLoader<ServiceFactory> serviceLoader;

  ServiceAdapterMultiFactory() {
    server = new Server();  // fixme: I don't like this. Maybe roll out some DI? Guice, for instance?
    serviceLoader = ServiceLoader.load(ServiceFactory.class);  // fixme: that makes it difficult to write Unit tests. Inject what?
  }

  public static ServiceAdapterMultiFactory getInstance() {
    return INSTANCE;
  }

  /**
   * Creates a new service.
   * Uses ServiceFactory service providers to load a class.
   */
  public UserServiceAdapter createService() {
    Iterator<ServiceFactory> iter = serviceLoader.iterator();
    checkState(iter.hasNext(), "No service factories available: " + serviceLoader);
    ServiceFactory serviceFactory = iter.next();
    return new UserServiceAdapter(serviceFactory.createService(), server);
  }

  /**
   * Creates a new service.
   * Uses reflection to load a factory class by FQN and instantiate it.
   */
  public UserServiceAdapter createService(String factoryFqn) {
    try {
      Object supposedlyFactory = Class.forName(factoryFqn).newInstance();
      checkArgument(supposedlyFactory instanceof ServiceFactory,
          "Class %s is not an instance of ServiceFactory", factoryFqn);
      ServiceFactory serviceFactory = (ServiceFactory) supposedlyFactory;
      return new UserServiceAdapter(serviceFactory.createService(), server);
    } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
