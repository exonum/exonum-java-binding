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

package com.exonum.binding.runtime;

import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Allows to check if a <em>copy</em> of a particular class can be loaded via the given classloader.
 * It uses the classes loaded by the <em>application classloader</em> as a reference; and
 * tries to load same classes with the <em>plugin classloader</em>. If they are same instances,
 * then there is no copy of class A on the classpath of plugin classloader; otherwise,
 * there is a copy of class A on the classpath of the plugin classloader (of same or distinct
 * version).
 *
 * <p><b>This class assumes that the plugin classloaders are parent-last, which is
 * {@linkplain org.pf4j.PluginClassLoader the case with PF4J}.</b>
 */
class ClassLoadingScopeChecker {

  static final String DEPENDENCY_REFERENCE_CLASSES_KEY = "Dependency reference classes";

  private final Map<String, Class<?>> dependencyReferenceClasses;

  /**
   * Creates a new classloading scope checker.
   *
   * @param dependencyReferenceClasses a reference class for each application dependency
   *     (e.g., {@code {"guava": ImmutableList.class}})
   */
  @Inject
  ClassLoadingScopeChecker(
      @Named(DEPENDENCY_REFERENCE_CLASSES_KEY) Map<String, Class<?>> dependencyReferenceClasses) {
    this.dependencyReferenceClasses = dependencyReferenceClasses;
  }

  // todo: withOptionalDependencies (recommended)?

  // todo: Shall we use "plugin classloader" (PF4J specific term) or "module classloader"?
  /**
   * Checks if there are copies of application classes on the classpath of the given classloader.
   * @param pluginClassloader a plugin parent-last classloader
   * @throws IllegalArgumentException if a copy of an application class is available on the
   *     classpath of the given classloader
   * @throws IllegalStateException if the given classloader fails to delegate
   *     to the application classloader
   */
  void checkNoCopiesOfAppClasses(ClassLoader pluginClassloader) {
    /*
    todo: Would you prefer the iterative version?
    List<String> libraryCopies = new ArrayList<>(0);
    for (Entry<String, Class<?>> entry : dependencyReferenceClasses.entrySet()) {
      String libraryName = entry.getKey();
      Class<?> referenceClass = entry.getValue();
      String referenceClassName = referenceClass.getName();
      try {
        Class<?> loadedThruPlugin = pluginClassloader.loadClass(referenceClassName);
        if (referenceClass != loadedThruPlugin) {
          libraryCopies.add(libraryName);
        }
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException(
            String.format("Classloader (%s) failed to load the reference "
                + "application class from %s library", pluginClassloader, libraryName), e);
      }
    }
    */

    List<String> libraryCopies = dependencyReferenceClasses.entrySet().stream()
        .filter(e -> loadsCopyOf(pluginClassloader, e))
        .map(Entry::getKey)
        .collect(toList());

    if (libraryCopies.isEmpty()) {
      return;
    }

    String message = String.format("Classloader (%s) loads copies of the following "
            + "libraries: %s.%n"
            + "Please ensure in your service build definition that each of these libraries:%n"
            + "  1. Has 'provided' scope%n"
            + "  2. Does not specify its version (i.e., inherits it "
            + "from exonum-java-binding-bom)%n"
            + "See also: "
            + "https://exonum.com/doc/version/0.10/get-started/java-binding/#using-libraries",
        pluginClassloader, libraryCopies);
    throw new IllegalArgumentException(message);
  }

  private boolean loadsCopyOf(ClassLoader pluginClassloader,
      Entry<String, Class<?>> referenceEntry) {
    String libraryName = referenceEntry.getKey();
    Class<?> referenceClass = referenceEntry.getValue();
    String referenceClassName = referenceClass.getName();
    try {
      Class<?> loadedThruPlugin = pluginClassloader.loadClass(referenceClassName);
      return referenceClass != loadedThruPlugin;
    } catch (ClassNotFoundException e) {
      String message = String.format("Classloader (%s) failed to load the reference "
              + "application class (%s) from %s library", pluginClassloader, referenceClass,
          libraryName);
      throw new IllegalStateException(message, e);
    }
  }
}
