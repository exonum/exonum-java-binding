package com.exonum.binding.service;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.service.adapters.UserServiceAdapter;
import com.exonum.binding.transport.Server;
import com.exonum.binding.util.LibraryLoader;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A Java service bootstrap loader.
 */
final class ServiceBootstrap {

  private static final Logger logger = LogManager.getLogger(ServiceBootstrap.class);

  /**
   * Bootstraps the Java service.
   *
   * @param serviceModuleName a fully-qualified class name of the user service module
   * @param serverPort a port to listen for connections on
   * @return a new service
   */
  @SuppressWarnings({"unused", "SameParameterValue"})  // Native API
  static UserServiceAdapter startService(String serviceModuleName, int serverPort) {
    try {
      // Create the injector.
      Injector injector = Guice.createInjector(new FrameworkModule(),
          createUserModule(serviceModuleName));

      // Start the HTTP server providing transport of requests.
      Server server = injector.getInstance(Server.class);
      Runtime.getRuntime().addShutdownHook(
          new Thread(() -> {
            try {
              server.stop().get();
            } catch (InterruptedException | ExecutionException e) {
              logger.warn("Failed to stop the server during VM shutdown", e);
            }
          })
      );
      server.start(serverPort);

      // Load the native libraries.
      LibraryLoader.load();

      // Instantiate the user service.
      return injector.getInstance(UserServiceAdapter.class);
    } catch (Throwable t) {
      logger.fatal("Failed to start service {}:", serviceModuleName, t);
      throw t;
    }
  }

  /**
   * Creates a user module that configures the bindings of that module.
   */
  private static Module createUserModule(String moduleName) {
    try {
      Class<?> moduleClass = Class.forName(moduleName);
      Constructor constructor = moduleClass.getDeclaredConstructor();
      Object moduleObject = constructor.newInstance();
      checkArgument(moduleObject instanceof Module, "%s is not a sub-class of %s",
          moduleClass, Module.class.getCanonicalName());
      return (Module) moduleObject;
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Module class cannot be found", e);
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("Cannot access the no-arg module constructor", e);
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("No no-arg constructor", e);
    } catch (InstantiationException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
