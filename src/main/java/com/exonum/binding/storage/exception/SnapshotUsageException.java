package com.exonum.binding.storage.exception;

public class SnapshotUsageException extends RuntimeException {

  public SnapshotUsageException() {
    super("Unpermitted action for Snapshot instance");
  }
}
