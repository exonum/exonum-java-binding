package com.exonum.binding.proxy;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singleton;

import java.util.Set;

/**
 * An utility class containing methods to check if a native proxy is valid.
 */
public final class ProxyPreconditions {

  /**
   * Checks that all native proxies are valid.
   *
   * @param nativeProxies a set of native proxies.
   * @throws NullPointerException if any proxy is null
   * @throws IllegalStateException if any proxy is invalid
   */
  public static void checkValid(Set<AbstractNativeProxy> nativeProxies) {
    for (AbstractNativeProxy proxy : nativeProxies) {
      checkState(proxy.isValid(), "Proxy is not valid: %s", proxy);
    }
  }

  /**
   * Checks that a native proxy is valid.
   *
   * @param nativeProxy a native proxy.
   * @throws NullPointerException if the proxy is null
   * @throws IllegalStateException if the proxy is invalid
   */
  public static void checkValid(AbstractNativeProxy nativeProxy) {
    checkValid(singleton(nativeProxy));
  }

  private ProxyPreconditions() {}
}
