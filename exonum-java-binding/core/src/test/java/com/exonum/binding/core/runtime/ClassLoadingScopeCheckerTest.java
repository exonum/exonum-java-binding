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

package com.exonum.binding.core.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.inject.Guice;
import io.vertx.core.Vertx;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ClassLoadingScopeCheckerTest {

  @Test
  void checkNoCopiesOfAppClasses() throws ClassNotFoundException {
    Class<?> referenceClass = Vertx.class;
    Map<String, Class<?>> referenceClasses = ImmutableMap.of(
        "vertx", referenceClass
    );

    ClassLoadingScopeChecker checker = new ClassLoadingScopeChecker(referenceClasses);

    ClassLoader classLoader = mock(ClassLoader.class);
    when(classLoader.loadClass(referenceClass.getName()))
        .thenReturn((Class) referenceClass);

    checker.checkNoCopiesOfAppClasses(classLoader);
  }

  @Test
  void checkNoCopiesOfAppClassesDetectsCopies() throws ClassNotFoundException {
    String dependency = "vertx";
    Class<?> referenceClass = Vertx.class;
    Map<String, Class<?>> referenceClasses = ImmutableMap.of(
        dependency, referenceClass
    );

    ClassLoadingScopeChecker checker = new ClassLoadingScopeChecker(referenceClasses);

    ClassLoader classLoader = mock(ClassLoader.class);
    // Whilst it is not possible for a ClassLoader to return a class that has different
    // binary name, we have to resort to it because it isn't (easily) possible to instantiate
    // a different class
    Class actual = Set.class;
    when(classLoader.loadClass(referenceClass.getName()))
        .thenReturn(actual);

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> checker.checkNoCopiesOfAppClasses(classLoader));

    assertThat(e).hasMessageContaining(dependency);
  }

  @Test
  void checkNoCopiesOfAppClassesDetectsAllCopies() throws ClassNotFoundException {
    Set<String> copiedLibraries = ImmutableSet.of("vertx", "gson");
    Set<String> nonCopiedLibraries = ImmutableSet.of("guice");
    Map<String, Class<?>> referenceClasses = ImmutableMap.of(
        "vertx", Vertx.class,
        "guice", Guice.class,
        "gson", Gson.class
    );
    assertThat(Sets.union(copiedLibraries, nonCopiedLibraries))
        .isEqualTo(referenceClasses.keySet());

    ClassLoadingScopeChecker checker = new ClassLoadingScopeChecker(referenceClasses);

    ClassLoader classLoader = mock(ClassLoader.class);
    // Whilst it is not possible for a ClassLoader to return a class that has different
    // binary name, we have to resort to it because it isn't (easily) possible to instantiate
    // a different class
    for (String library : copiedLibraries) {
      Class actual = Set.class;
      Class<?> referenceClass = referenceClasses.get(library);
      when(classLoader.loadClass(referenceClass.getName()))
          .thenReturn(actual);
    }
    for (String library : nonCopiedLibraries) {
      Class actual = referenceClasses.get(library);
      when(classLoader.loadClass(actual.getName()))
          .thenReturn(actual);
    }

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> checker.checkNoCopiesOfAppClasses(classLoader));

    for (String libraryName : copiedLibraries) {
      assertThat(e).hasMessageContaining(libraryName);
    }
    for (String libraryName : nonCopiedLibraries) {
      assertThat(e).hasMessageNotContaining(libraryName);
    }
  }

  @Test
  void checkNoCopiesOfAppClassesClassloaderFailsToDelegate() throws ClassNotFoundException {
    Class<?> referenceClass = Vertx.class;
    Map<String, Class<?>> referenceClasses = ImmutableMap.of(
        "vertx", referenceClass
    );

    ClassLoadingScopeChecker checker = new ClassLoadingScopeChecker(referenceClasses);

    ClassLoader pluginClassLoader = mock(ClassLoader.class);
    when(pluginClassLoader.loadClass(referenceClass.getName()))
        .thenThrow(ClassNotFoundException.class);

    Exception e = assertThrows(IllegalStateException.class,
        () -> checker.checkNoCopiesOfAppClasses(pluginClassLoader));

    assertThat(e).hasMessageFindingMatch("Classloader .+ failed to load the reference "
        + "application class .+ from vertx library");
  }
}
