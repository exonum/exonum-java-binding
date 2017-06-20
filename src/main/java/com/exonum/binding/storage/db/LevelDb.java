package com.exonum.binding.storage.db;

import com.exonum.binding.storage.connector.Fork;
import com.exonum.binding.storage.connector.Snapshot;

public class LevelDb implements Database {

  private final long nativeLevelDb;

  public LevelDb(String pathToDb) {
    this.nativeLevelDb = nativeCreate(pathToDb);
  }

  @Override
  public void destroyNativeDb() {
    nativeFree(nativeLevelDb);
  }

  @Override
  public Snapshot lookupSnapshot() {
    return new Snapshot(nativeLookupSnapshot(this.nativeLevelDb));
  }

  @Override
  public Fork lookupFork() {
    return new Fork(nativeLookupFork(this.nativeLevelDb));
  }

  private native long nativeLookupSnapshot(long nativeDb);

  private native long nativeLookupFork(long nativeDb);

  private native long nativeCreate(String path);

  private native void nativeFree(long nativeDb);
}
