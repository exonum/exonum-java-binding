package com.exonum.binding.storage.database;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.proxy.ProxyContext;

/**
 * Represents a view of a database.
 *
 * <p>There are two sub-types:
 * <ul>
 *   <li>A snapshot, which is a <em>read-only</em> view.</li>
 *   <li>A fork, which is a <em>read-write</em> view.</li>
 * </ul>
 *
 * @see Snapshot
 * @see Fork
 */
public abstract class View<ViewProxyT extends ViewProxy> {

  private final ViewProxyT proxy;
  private final ProxyContext context;

  View(ViewProxyT proxy, ProxyContext context) {
    this.proxy = checkNotNull(proxy);
    this.context = context;

    context.add(proxy);
  }

  /**
   * Returns the corresponding view proxy.
   */
  public final ViewProxyT getProxy() {
    return proxy;
  }

  /**
   * Returns the context of this view.
   */
  public final ProxyContext getContext() {
    return context;
  }
}
