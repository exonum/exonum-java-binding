package com.google.common.hash;

/**
 * HashCode funnel. Puts the hash code bytes into the sink without copying.
 */
public enum HashCodeFunnel implements Funnel<HashCode> {
  INSTANCE;

  @Override
  public void funnel(HashCode from, PrimitiveSink into) {
    into.putBytes(from.getBytesInternal());
  }

  public static Funnel<HashCode> hashCodeFunnel() {
    return HashCodeFunnel.INSTANCE;
  }

}
