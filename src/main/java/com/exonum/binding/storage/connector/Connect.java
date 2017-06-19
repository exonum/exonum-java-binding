package com.exonum.binding.storage.connector;

import com.exonum.binding.NativeHandle;

public interface Connect extends NativeHandle {

  public void lockWrite();

  public void lockRead();

  public void unlockWrite();

  public void unlockRead();
}
