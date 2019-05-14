/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A loader of the native shared library with Exonum framework bindings. It loads the native
 * library and also verifies that it is compatible with the Java classes. The native library
 * is compatible iff it has exactly the same version as this Java library. The revision
 * from which they were built is currently not checked, but may be in the future (see ECR-3173),
 * because the API between Java and native is considered internal and can be changed
 * in an incompatible way in any revision.
 *
 * <p>To enable loading of java_bindings library, add a path to the folder containing it
 * to <code>java.library.path</code> property, e.g.:
 * <code>java -Djava.library.path=${EXONUM_HOME}/lib/native …</code>
 *
 * <p>This class is thread-safe.
 *
 * @see <a href="https://exonum.com/doc/version/0.11/get-started/java-binding/#installation">
 *   Exonum Java installation instructions</a>
 * @see <a href="https://exonum.com/doc/version/0.11/get-started/java-binding/#testing">
 *   Build configuration to enable integration testing of Java services</a>
 */
public final class LibraryLoader {

  private static final String BINDING_LIB_NAME = "java_bindings";
  private static final String JAVA_LIBRARY_PATH_PROPERTY = "java.library.path";
  private static final String DYNAMIC_LIBRARIES_ENV_VAR_WINDOWS = "PATH";
  private static final String DYNAMIC_LIBRARIES_ENV_VAR_UNIX = "LD_LIBRARY_PATH";

  /**
   * The current version of the project. Must be updated on
   * <a href="https://wiki.bf.local/display/EJB/Java+Binding+Release+Checklist+Template">
   * each release</a>.
   */
  private static final String JAVA_BINDING_VERSION = "0.7.0-SNAPSHOT";

  // TODO: Remove in ECR-3172
  private static final boolean LIBRARY_VERSION_VERIFICATION_ENABLED = false;

  private static final Logger logger = LogManager.getLogger(LibraryLoader.class);

  private static final LibraryLoader INSTANCE = new LibraryLoader(JAVA_BINDING_VERSION);

  private final String expectedLibVersion;
  private boolean loaded;

  /**
   * Creates a new library loader.
   *
   * @param libraryVersion the version of this library to verify that the native library
   *     is compatible with it
   */
  private LibraryLoader(String libraryVersion) {
    this.expectedLibVersion = libraryVersion;
    this.loaded = false;
  }

  /**
   * Loads the native library with Exonum framework bindings.
   *
   * @throws LinkageError if the native library cannot be loaded; or if it is incompatible
   *     with this library version
   */
  public static void load() {
    INSTANCE.loadOnce();
  }

  private synchronized void loadOnce() {
    if (loaded) {
      // It has already been attempted to load the library (successfully or not)
      return;
    }

    try {
      // Try to load the library
      loadLibrary();

      // Check that it has the compatible version
      checkLibraryVersion();
    } finally {
      loaded = true;
    }
  }

  private static void loadLibrary() {
    try {
      System.loadLibrary(BINDING_LIB_NAME);
    } catch (UnsatisfiedLinkError e) {
      String message = String.format("Failed to load '%s' library: %s.%n%s", BINDING_LIB_NAME,
          e.getMessage(), extraLibLoadErrorInfo());
      logger.fatal(message, e);

      // Throw a new exception with a _full_ error message so that it is always available,
      // even if the logger is not configured.
      throw new LinkageError(message, e);
    }
  }

  private static String extraLibLoadErrorInfo() {
    String javaLibPath = System.getProperty(JAVA_LIBRARY_PATH_PROPERTY);
    if (runningUnitTests()) {
      // 1. These error messages are tailored for an installed application. If you *develop*
      // Exonum, the instructions might not be applicable.
      // 2. java.library.path has a default platform-specific value if not specified (on 11),
      // hence it might never be empty.
      return String.format(
          "Check that %s system property includes a path to '${EXONUM_HOME}/lib/native' directory%n"
              + "containing %s library, where 'EXONUM_HOME' denotes the Exonum Java app "
              + "installation directory.%n"
              + "The code launching tests must usually set this property explicitly, see "
              + "https://exonum.com/doc/version/0.11/get-started/java-binding/#testing",
          JAVA_LIBRARY_PATH_PROPERTY, BINDING_LIB_NAME);
    } else {
      String dynamicLibVar = dynamicLibrariesEnvVar();
      String dynamicLibValue = System.getenv(dynamicLibVar);
      return String.format("Unknown error: please submit an issue including this error message.%n"
              + "%s=%s%n" + "%s=%s", JAVA_LIBRARY_PATH_PROPERTY, javaLibPath,
          dynamicLibVar, dynamicLibValue);
    }
  }

  private static boolean runningUnitTests() {
    try {
      Class.forName("org.junit.jupiter.api.Test");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private static String dynamicLibrariesEnvVar() {
    if (OsInfo.isWindows()) {
      return DYNAMIC_LIBRARIES_ENV_VAR_WINDOWS;
    } else {
      return DYNAMIC_LIBRARIES_ENV_VAR_UNIX;
    }
  }

  private void checkLibraryVersion() {
    if (!LIBRARY_VERSION_VERIFICATION_ENABLED) {
      return;
    }
    String nativeLibVersion = nativeGetLibraryVersion();
    if (!expectedLibVersion.equals(nativeLibVersion)) {
      String message = String.format(
          "Mismatch between versions of Java library and native '%s' library:%n"
              + "  Java library version:   %s%n"
              + "  Native library version: %s%n"
              + "Check that the version of 'exonum-java-binding-core' matches the version of "
              + "the installed 'Exonum Java' application.%n"
              + "See https://exonum.com/doc/version/0.11/get-started/java-binding/#installation",
          BINDING_LIB_NAME, expectedLibVersion, nativeLibVersion);
      logger.fatal(message);
      throw new LinkageError(message);
    }
  }

  private static native String nativeGetLibraryVersion();
}
