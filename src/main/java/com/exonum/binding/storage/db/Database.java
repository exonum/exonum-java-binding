package com.exonum.binding.storage.db;

import com.exonum.binding.storage.connector.Fork;
import com.exonum.binding.storage.connector.Snapshot;

public interface Database {

  public Snapshot lookupSnapshot();

  public Fork lookupFork();

  public void destroyNativeDb();
}
