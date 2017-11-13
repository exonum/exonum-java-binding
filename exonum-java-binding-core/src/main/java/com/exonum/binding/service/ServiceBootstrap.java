package com.exonum.binding.service;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.transport.Server;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import java.util.concurrent.ExecutionException;

/**
 * This class is intended to be used by native code to bootstrap a Java service.
 */
class ServiceBootstrap {

  /**
   * Bootstraps the Java service.
   *
   * @param serviceModuleName a module name of the user service
   * @param serverPort a port to listen for connections on
   * @return a new service
   */
  static UserServiceAdapter startService(String serviceModuleName, int serverPort) {
    Injector injector = Guice.createInjector(new MyModule(), createUserModule(serviceModuleName));

    Server server = injector.getInstance(Server.class);
    Runtime.getRuntime().addShutdownHook(
        new Thread(() -> {
          try {
            server.stop().get();
          } catch (InterruptedException | ExecutionException e) {
            System.err.println("Failed to stop the server during VM shutdown: "
                + e.getMessage());
          }
        })
    );
    server.start(serverPort);

    return injector.getInstance(UserServiceAdapter.class);
  }

  /**
   * Creates a user module that configures the bindings of that module.
   */
  private static Module createUserModule(String moduleName) {
    try {
      Class<?> moduleClass = Class.forName(moduleName);
      Object moduleObject = moduleClass.newInstance();
      checkArgument(moduleObject instanceof Module, "Class is not a sub-class of %s",
          Module.class.getCanonicalName());
      return (Module) moduleObject;
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Module class cannot be found", e);
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("Cannot access the no-arg module constructor", e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }
}

/**
 * A framework module which configures the system-wide bindings.
 */
class MyModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Server.class).in(Singleton.class);
    //  fixme: if that's a proxy of a NodeProxy :-)
//    bind(Node.class);
    bind(UserServiceAdapter.class);
  }
}
