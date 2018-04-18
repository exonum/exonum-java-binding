package com.exonum.binding.proxy;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.MoreObjects;
import java.util.ArrayDeque;
import java.util.Deque;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A context controlling lifecycle of native proxies. Native proxies must be registered in a context
 * when created. The context destroys all registered proxies in a reversed order of their
 * registration when it is {@linkplain #close() closed}.
 *
 * <p>All methods are non-null by default.
 *
 * <p>This class is not thread-safe.
 */
public final class ProxyContext implements AutoCloseable {

  private static final Logger logger = LogManager.getLogger(ProxyContext.class);

  /**
   * The number of registered proxies at which we start to log warnings when more are added
   * at {@link #TOO_MANY_PROXIES_LOG_FREQUENCY}.
   *
   * @see #logIfTooManyProxies()
   */
  private static final int TOO_MANY_PROXIES_LOG_THRESHOLD = 1000;
  private static final int TOO_MANY_PROXIES_LOG_FREQUENCY = 100;

  private final Deque<NativeProxy> registeredProxies;
  private boolean closed;

  public ProxyContext() {
    registeredProxies = new ArrayDeque<>();
    closed = false;
  }

  /**
   * Registers a new proxy with this context.
   *
   * @param proxy a proxy to register; must not be null
   *
   * @throws IllegalStateException if it’s attempted to add a proxy to a closed context
   */
  public void add(NativeProxy proxy) {
    checkState(!closed, "Cannot register a proxy (%s) in a closed context", proxy);
    registeredProxies.push(proxy);

    // As this class is used to automatically (from the user perspective) manage resources,
    // we log if there is an unusually high number of native proxies.
    logIfTooManyProxies();
  }

  private void logIfTooManyProxies() {
    int numRegisteredProxies = registeredProxies.size();

    if ((numRegisteredProxies >= TOO_MANY_PROXIES_LOG_THRESHOLD)
        && (numRegisteredProxies % TOO_MANY_PROXIES_LOG_FREQUENCY == 0)) {
      // todo: is it an overkill?
      String proxiesByTypeFrequency = FrequencyStatsFormatter
          .itemsByTypeFrequency(registeredProxies);
      logger.warn("Many proxies (%d) are registered in a context (%s): [%s]",
          numRegisteredProxies, this, proxiesByTypeFrequency);
    }
  }

  /**
   * Destroys all the native proxies that has been created in this context. The proxies
   * are destroyed in a reversed order of the registration order.
   *
   * <p>If any registered proxy throws an exception in its {@link NativeProxy#close()},
   * the context logs the exceptions and attempts to close the remaining proxies.
   *
   * <p>The implementation is idempotent — subsequent invocations have no effect.
   *
   * @throws CloseFailuresException if any proxy was not closed gracefully
   */
  @Override
  public void close() throws CloseFailuresException {
    if (closed) {
      return;
    }

    closed = true;

    // Currently only the number of failures is recorded. If extra context is needed,
    // the proxies might be included as well.
    int numFailures = 0;
    while (!registeredProxies.isEmpty()) {
      NativeProxy proxy = registeredProxies.pop();
      // Try to close the proxy.
      try {
        proxy.close();
      } catch (Throwable t) {
        // Record the failure
        numFailures++;
        // Log the details
        String message = String.format("Exception occurred when context (%s) attempted to close "
            + "a native proxy (%s): ", this, proxy);
        logger.error(message, t);
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
        .add("numRegisteredProxies", registeredProxies.size())
        .add("closed", closed)
        .toString();
  }

  // todo: diagnostic info
}
