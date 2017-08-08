package com.exonum.binding.storage.database;

final class Views {
  /** Destroys the native `View` object. May be used with both Snapshots and Forks. */
  static native void nativeFree(long viewNativeHandle);

  private Views() {}
}
