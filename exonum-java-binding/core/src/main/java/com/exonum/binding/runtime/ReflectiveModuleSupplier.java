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

import com.exonum.binding.service.ServiceModule;
import com.google.common.base.MoreObjects;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;

// todo: such implementation is nicer in terms of error handling (it happens upfront), but
//   does not allow package-private module until Java 9 with MethodHandles#privateLookupIn
//   [ECR-3008]

/**
 * A reflective supplier of service modules that instantiates them with a no-arg constructor.
 */
public final class ReflectiveModuleSupplier implements Supplier<ServiceModule> {

  private final Class<? extends ServiceModule> moduleClass;
  private final MethodHandle moduleConstructor;

  public ReflectiveModuleSupplier(Class<? extends ServiceModule> moduleClass)
      throws NoSuchMethodException, IllegalAccessException {
    this.moduleClass = moduleClass;
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    MethodType mt = MethodType.methodType(void.class);
    moduleConstructor = lookup.findConstructor(moduleClass, mt);
  }

  @Override
  public ServiceModule get() {
    return newServiceModule();
  }

  private ServiceModule newServiceModule() {
    try {
      return (ServiceModule) moduleConstructor.invoke();
    } catch (Throwable throwable) {
      String message = String
          .format("Cannot instantiate a service module of class %s using constructor %s",
              moduleClass, moduleConstructor);
      throw new IllegalStateException(message, throwable);
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("moduleClass", moduleClass)
        .add("moduleConstructor", moduleConstructor)
        .toString();
  }
}
