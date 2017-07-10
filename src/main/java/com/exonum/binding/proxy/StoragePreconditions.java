package com.exonum.binding.proxy;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singleton;

import java.util.Set;

class StoragePreconditions {

  private static final int MIN_INDEX_PREFIX_LENGTH = 1;

  /**
   * Checks that an index prefix is valid.
   *
   * @param prefix an index prefix.
   * @return an unmodified prefix if it's valid.
   * @throws NullPointerException if the prefix is null.
   * @throws IllegalArgumentException if the prefix has zero length.
   */
  static byte[] checkIndexPrefix(byte[] prefix) {
    checkArgument(MIN_INDEX_PREFIX_LENGTH <= prefix.length, "prefix has zero size");
    return prefix;
  }

  /**
   * Checks that a key is valid.
   *
   * @param key a storage key.
   * @return an unmodified key if it's valid.
   * @throws NullPointerException if key is null.
   */
  static byte[] checkStorageKey(byte[] key) {
    return checkNotNull(key, "Storage key is null");
  }

  /**
   * Checks that a value is valid.
   *
   * @param value a storage value.
   * @return an unmodified value if it's valid.
   * @throws NullPointerException if value is null.
   */
  static byte[] checkStorageValue(byte[] value) {
    return checkNotNull(value, "Storage value is null");
  }

  /**
   * Checks that all native proxies are valid.
   *
   * @param nativeProxies a set of native proxies.
   * @throws NullPointerException if any proxy is null
   * @throws IllegalStateException if any proxy is invalid
   */
  static void checkValid(Set<AbstractNativeProxy> nativeProxies) {
    for (AbstractNativeProxy proxy : nativeProxies) {
      checkState(proxy.isValid(), "Proxy is not valid: " + proxy);
    }
  }

  /**
   * Checks that a native proxy is valid.
   *
   * @param nativeProxy a native proxy.
   * @throws NullPointerException if the proxy is null
   * @throws IllegalStateException if the proxy is invalid
   */
  static void checkValid(AbstractNativeProxy nativeProxy) {
    checkValid(singleton(nativeProxy));
  }
}
