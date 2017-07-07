package com.exonum.binding.proxy;

import com.exonum.binding.annotations.ImproveDocs;

/**
 * A fork is a database view, allowing both read and write operations.
 *
 * <p>A fork allows to perform a transaction: a number of independent writes to a database,
 * which then may be <em>atomically</em> applied to the database state.
 */
@ImproveDocs(assignee = "dt", reason = "Document managed/non-managed forks when we have them")
public class Fork extends View {

  public Fork(long nativeHandle) {
    super(nativeHandle, true);
  }

  @Override
  void disposeInternal() {
    Views.nativeFree(nativeHandle);
  }
}
