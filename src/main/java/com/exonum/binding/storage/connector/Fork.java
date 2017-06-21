package com.exonum.binding.storage.connector;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Fork implements Connect {

  private static ReadWriteLock locker = new ReentrantReadWriteLock();

  private final long nativeFork;

  public Fork(long forkObj) {
    this.nativeFork = forkObj;
  }

  @Override
  public long getNativeHandle() {
    return nativeFork;
  }

  @Override
  public void close() {
    Views.nativeFree(nativeFork);
  }

  @Override
  public void lockWrite() {
    locker.writeLock().lock();
  }

  @Override
  public void lockRead() {
    locker.readLock().lock();
  }

  @Override
  public void unlockWrite() {
    locker.writeLock().unlock();
  }

  @Override
  public void unlockRead() {
    locker.readLock().unlock();
  }
}
