package com.exonum.binding.proxy;

import com.google.common.base.MoreObjects;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A context controlling lifecycle of native proxies. When a proxy of a native object is created,
 * it must register a cleaner of the native object in a context.
 * The context performs the cleaning actions in a reversed order of their registration
 * when it is {@linkplain #close() closed}.
 *
 * <p>All methods are non-null by default.
 *
 * <p>This class is not thread-safe.
 */
public final class Cleaner implements AutoCloseable {

  private static final Logger logger = LogManager.getLogger(Cleaner.class);

  /**
   * The number of registered clean actions at which we start to log warnings when more are added
   * at {@link #TOO_MANY_CLEAN_ACTIONS_LOG_FREQUENCY}.
   *
   * @see #logIfTooManyCleaners()
   */
  private static final int TOO_MANY_CLEAN_ACTIONS_LOG_THRESHOLD = 1000;
  private static final int TOO_MANY_CLEAN_ACTIONS_LOG_FREQUENCY = 100;

  private final Deque<CleanAction> registeredCleanActions;
  private boolean closed;

  public Cleaner() {
    registeredCleanActions = new ArrayDeque<>();
    closed = false;
  }

  /**
   * Registers a new clean action with this context. If the context is already closed,
   * the clean action will be executed immediately.
   *
   * @param cleanAction a clean action to register; must not be null
   *
   * @throws IllegalStateException if it’s attempted to add a clean action to a closed context
   */
  public void add(CleanAction cleanAction) {
    if (closed) {
      // To avoid possible leaks, perform the clean action before throwing IllegalStateException.
      Throwable cleanActionError = null;
      try {
        cleanAction.clean();
      } catch (Throwable t) {
        logCleanActionFailure(cleanAction, t);
        cleanActionError = t;
      }

      String message = String.format("Cannot register a clean action (%s) in a closed context",
          cleanAction);
      RuntimeException e = new IllegalStateException(message);
      if (cleanActionError != null) {
        e.addSuppressed(cleanActionError);
      }
      throw e;
    }

    registeredCleanActions.push(cleanAction);

    // As this class is used to automatically (from the user perspective) manage resources,
    // we log if there is an unusually high number of resource cleaners.
    logIfTooManyCleaners();
  }

  private void logIfTooManyCleaners() {
    int numRegisteredCleaners = registeredCleanActions.size();

    if ((numRegisteredCleaners >= TOO_MANY_CLEAN_ACTIONS_LOG_THRESHOLD)
        && (numRegisteredCleaners % TOO_MANY_CLEAN_ACTIONS_LOG_FREQUENCY == 0)) {

      String proxiesByTypeFrequency =
          FrequencyStatsFormatter.itemsFrequency(registeredCleanActions, Cleaner::getActionType);

      logger.warn("Many cleaners ({}) are registered in a context ({}): {}",
          numRegisteredCleaners, this, proxiesByTypeFrequency);
    }
  }

  private static Object getActionType(CleanAction<Object> a) {
    Optional<Object> rt = a.resourceType();
    return rt.orElse("Unknown");
  }

  /**
   * Performs all the clean operations that has been registered in this context in a reversed order
   * of the registration order.
   *
   * <p>If any clean operation throws an exception in its {@link CleanAction#clean()},
   * the context logs the exception and attempts to perform the remaining operations.
   *
   * <p>The implementation is idempotent — subsequent invocations have no effect.
   *
   * @throws CloseFailuresException if any clean action failed
   */
  @Override
  public void close() throws CloseFailuresException {
    if (closed) {
      return;
    }

    closed = true;

    // Currently only the number of failures is recorded. If extra context is needed,
    // the clean actions might be included as well.
    int numFailures = 0;
    while (!registeredCleanActions.isEmpty()) {
      CleanAction cleanAction = registeredCleanActions.pop();
      // Try to perform the operation.
      try {
        cleanAction.clean();
      } catch (Throwable t) {
        // Record the failure
        numFailures++;
        // Log the details
        logCleanActionFailure(cleanAction, t);
        // todo: Shall I throw Errors immediately: Throwables.throwIfInstanceOf(t, Error.class);
      }
    }

    // If there has been any failures, throw an exception with a detailed error message.
    if (numFailures != 0) {
      String message = String.format("%d exception(s) occurred when closing this context (%s), "
          + "see the log messages above", numFailures, this);
      throw new CloseFailuresException(message);
    }
  }

  private void logCleanActionFailure(CleanAction cleanAction, Throwable cleanException) {
    logger.error("Exception occurred when this context ({}) attempted to perform "
        + "a clean operation ({}):", this, cleanAction, cleanException);
  }

  /**
   * Returns a string representation of this object, including its hash code so that this instance
   * can be easily identified in the logs.
   */
  @Override
  public String toString() {
    // Fixme: Use an id instead of hash: a unique one?
    String hash = Integer.toHexString(System.identityHashCode(this));
    return MoreObjects.toStringHelper(this)
        .add("hash", hash)
        .add("numRegisteredActions", registeredCleanActions.size())
        .add("closed", closed)
        .toString();
  }

  /**
   * Returns the number of the registered clean actions.
   */
  public int getNumRegisteredActions() {
    return registeredCleanActions.size();
  }

  // todo: more diagnostic info?
}
