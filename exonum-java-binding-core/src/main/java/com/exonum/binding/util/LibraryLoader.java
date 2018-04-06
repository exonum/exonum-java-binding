package com.exonum.binding.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A loader of the native shared library with Exonum framework bindings.
 *
 * <p>To have library java_bindings available by its name,
 * add a path to the folder containing it to <code>java.library.path</code> property,
 * e.g.: <code>java -Djava.library.path=rust/target/release …</code>
 */
public final class LibraryLoader {

  private static final String BINDING_LIB_NAME = "java_bindings";
  private static final String JAVA_LIBRARY_PATH_PROPERTY = "java.library.path";
  private static final String DYNAMIC_LIBRARIES_ENV_VAR = "LD_LIBRARY_PATH";

  private static final Logger logger = LogManager.getLogger(LibraryLoader.class);

  /**
   * Loads the native library with Exonum framework bindings.
   */
  public static void load() {
    loadOnce();
  }

  private static void loadOnce() {
    try {
      System.loadLibrary(BINDING_LIB_NAME);
    } catch (UnsatisfiedLinkError e) {
      logger.error("Failed to load '{}' library: {}…\n{}",
          BINDING_LIB_NAME, e, extraLibLoadErrorInfo());
      throw e;
    }
  }

  private static String extraLibLoadErrorInfo() {
    String javaLibPath = System.getProperty(JAVA_LIBRARY_PATH_PROPERTY);
    String dynamicLibPath = System.getenv(DYNAMIC_LIBRARIES_ENV_VAR);
    // todo: clarify this message when LD_LIBRARY_PATH becomes required.
    return "java.library.path=" + javaLibPath + ", \n"
        + DYNAMIC_LIBRARIES_ENV_VAR + "=" + dynamicLibPath
        + "\nMake sure that:\n"
        + "1. The path to a directory containing '" + BINDING_LIB_NAME
        + "' dynamic library image is included in either java.library.path system property or "
        + DYNAMIC_LIBRARIES_ENV_VAR + " environment variable.\n"
        + "2. The paths to directories containing dynamic libraries required by '"
        + BINDING_LIB_NAME + "', if any, are included in " + DYNAMIC_LIBRARIES_ENV_VAR
        + " environment variable";
  }

  private LibraryLoader() {}
}
