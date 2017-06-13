package com.exonum.binding.storage.connector;

public interface Connect {

  public void lockWrite();

  public void lockRead();

  public void unlockWrite();

  public void unlockRead();

  public void destroyNativeConnect();
}
