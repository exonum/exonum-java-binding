package com.exonum.binding.proxy;

import com.exonum.binding.annotations.ImproveDocs;

@ImproveDocs(
    assignee = "Timofeev",
    reason = "Lacks in both general description and method documentation."
)
public abstract class Database extends AbstractNativeProxy {

  /**
   * @param nativeHandle a native handle: an implementation-specific reference to a native object.
   * @param owningHandle true if this proxy is responsible to release any native resources;
   */
  Database(long nativeHandle, boolean owningHandle) {
    super(nativeHandle, owningHandle);
  }

  public abstract Snapshot lookupSnapshot();

  public abstract Fork lookupFork();
}
