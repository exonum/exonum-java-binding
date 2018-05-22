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
