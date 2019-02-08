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

package com.exonum.binding.proxy;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

/**
 * A clean action is an operation that is performed to release some resources.
 * The type of resource may be optionally specified.
 *
 * @param <ResourceDescriptionT> type of resource this action cleans (usually, an instance
 *                               of {@link java.lang.Class}, {@link String}, {@link Enum}),
 */
@FunctionalInterface
public interface CleanAction<ResourceDescriptionT> {

  /**
   * A clean operation to perform. It is recommended that this operation is idempotent.
   */
  void clean();

  /**
   * Returns the description of the type of resource this action corresponds to.
   */
  default Optional<ResourceDescriptionT> resourceType() {
    return Optional.empty();
  }

  /**
   * Creates a clean action with a given type.
   *
   * @param action a clean operation
   * @param resourceType a description of the resource (its class, textual description, etc.)
   * @param <ResourceDescriptionT> a type of the resource description
   */
  static <ResourceDescriptionT>
      CleanAction<ResourceDescriptionT> from(Runnable action,
                                             ResourceDescriptionT resourceType) {
    checkNotNull(resourceType, "resourceType must not be null");

    return new CleanAction<ResourceDescriptionT>() {
      @Override
      public void clean() {
        action.run();
      }

      @Override
      public Optional<ResourceDescriptionT> resourceType() {
        return Optional.of(resourceType);
      }
    };
  }
}
