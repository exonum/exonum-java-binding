package com.exonum.binding.storage.database;

import com.exonum.binding.proxy.ProxyContext;

public final class Snapshot extends View<SnapshotProxy> {

  public Snapshot(SnapshotProxy proxy, ProxyContext context) {
    super(proxy, context);
  }
}
