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

import com.google.common.base.MoreObjects;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;
import org.pf4j.ExtensionPoint;

/**
 * A reflective supplier of service modules that instantiates them with a no-arg constructor.
 */
public final class ReflectiveExtensionSupplier<T extends ExtensionPoint> implements Supplier<T> {

  private final Class<? extends T> extensionClass;
  private final MethodHandle extensionConstructor;

  /**
   * Creates a module supplier for a given service module class.
   *
   * @throws NoSuchMethodException if the constructor of given service module class does not exist
   * @throws IllegalAccessException if accessing the no-arg module constructor failed
   */
  public ReflectiveExtensionSupplier(Class<? extends T> extensionClass)
      throws NoSuchMethodException, IllegalAccessException {
    this.extensionClass = extensionClass;
    MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(extensionClass,
        MethodHandles.lookup());
    MethodType mt = MethodType.methodType(void.class);
    extensionConstructor = lookup.findConstructor(extensionClass, mt);
  }

  @Override
  public T get() {
    return initializeExtension();
  }

  private T initializeExtension() {
    try {
      return (T) extensionConstructor.invoke();
    } catch (Throwable throwable) {
      String message = String
          .format("Cannot instantiate extension of class %s using constructor %s",
              extensionClass, extensionConstructor);
      throw new IllegalStateException(message, throwable);
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("extensionClass", extensionClass)
        .add("extensionConstructor", extensionConstructor)
        .toString();
  }
}
