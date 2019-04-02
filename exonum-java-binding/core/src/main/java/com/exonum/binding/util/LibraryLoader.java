/*
 * Copyright 2018 The Exonum Team
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
 * A loader of the native shared library with Exonum framework bindings.
 *
 * <p>To have library java_bindings available by its name,
 * add a path to the folder containing it to <code>java.library.path</code> property,
 * e.g.: <code>java -Djava.library.path=${EXONUM_HOME}/lib/native …</code>
 */
public final class LibraryLoader {

  private static final String BINDING_LIB_NAME = "java_bindings";
  private static final String JAVA_LIBRARY_PATH_PROPERTY = "java.library.path";
  private static final String DYNAMIC_LIBRARIES_ENV_VAR_WINDOWS = "PATH";
  private static final String DYNAMIC_LIBRARIES_ENV_VAR_UNIX = "LD_LIBRARY_PATH";

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
              + "containing %s library, where 'EXONUM_HOME' denotes the Exonum Java installation "
              + "directory.%n"
              + "The code launching tests must usually set it explicitly, see "
              + "https://exonum.com/doc/version/0.10/get-started/java-binding/#testing",
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

  private LibraryLoader() {}
}
