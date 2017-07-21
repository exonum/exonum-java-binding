package com.exonum.binding.util;

/**
 * A loader of the native shared library with Exonum framework bindings.
 *
 * <p>To have library java_bindings available by its name,
 * add a path to the folder containing it to <code>java.library.path</code> property,
 * e.g.: <code>java -Djava.library.path=rust/target/release …</code>
 */
public final class LibraryLoader {

  private static final String BINDINGS_LIB_NAME = "java_bindings";

  /**
   * Loads the native library with Exonum framework bindings.
   */
  public static void load() {
    loadOnce();
  }

  private static void loadOnce() {
    System.loadLibrary(BINDINGS_LIB_NAME);
  }
}
