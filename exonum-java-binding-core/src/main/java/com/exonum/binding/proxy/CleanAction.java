package com.exonum.binding.proxy;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

/**
 * A clean action is an operation that is performed to release some resources.
 * The type of resource may be optionally specified.
 *
 * @param <ResourceTypeT> type of resource this action cleans (usually, an instance
 *                       of {@link java.lang.Class}, {@link String}, {@link Enum}),
 */
@FunctionalInterface
// fixme: Q: Or "ResourceDescriptionT"?
public interface CleanAction<ResourceTypeT> {

  /**
   * A clean operation to perform. It is recommended that this operation is idempotent.
   */
  void clean();

  /**
   * Returns the description of the type of resource this action corresponds to.
   */
  default Optional<ResourceTypeT> resourceType() {
    return Optional.empty();
  }

  /**
   * Creates a clean action with a given type.
   *
   * @param action a clean operation
   * @param resourceType a description of the resource (its class, textual description, etc.)
   * @param <ResourceTypeT> a type of the resource description
   */
  static <ResourceTypeT> CleanAction<ResourceTypeT> from(Runnable action,
                                                         ResourceTypeT resourceType) {
    checkNotNull(resourceType, "resourceType must not be null");

    return new CleanAction<ResourceTypeT>() {
      @Override
      public void clean() {
        action.run();
      }

      @Override
      public Optional<ResourceTypeT> resourceType() {
        return Optional.of(resourceType);
      }
    };
  }
}
