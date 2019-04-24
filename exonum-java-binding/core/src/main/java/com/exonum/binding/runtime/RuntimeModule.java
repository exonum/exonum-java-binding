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

import static com.exonum.binding.runtime.ClassLoadingScopeChecker.DEPENDENCY_REFERENCE_CLASSES_KEY;
import static com.google.inject.name.Names.named;

import com.google.common.collect.ImmutableMap;
import com.google.inject.PrivateModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import java.util.Map;
import org.pf4j.PluginManager;

/**
 * A module for the runtime package. Exposes {@link ServiceRuntime} only.
 */
final class RuntimeModule extends PrivateModule {

  private final Map<String, Class<?>> dependencyReferenceClasses;

  RuntimeModule(Map<String, Class<?>> dependencyReferenceClasses) {
    this.dependencyReferenceClasses = ImmutableMap.copyOf(dependencyReferenceClasses);
  }

  @Override
  protected void configure() {
    bind(ServiceRuntime.class).in(Singleton.class);
    expose(ServiceRuntime.class);

    bind(ClassLoadingScopeChecker.class);
    bind(new TypeLiteral<Map<String, Class<?>>>() {})
        .annotatedWith(named(DEPENDENCY_REFERENCE_CLASSES_KEY))
        .toInstance(dependencyReferenceClasses);
    bind(ServiceLoader.class).to(Pf4jServiceLoader.class);
    bind(PluginManager.class).to(JarPluginManager.class);
  }
}
