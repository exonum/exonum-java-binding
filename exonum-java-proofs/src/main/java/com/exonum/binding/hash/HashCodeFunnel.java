package com.exonum.binding.hash;

/**
 * HashCode funnel. Puts the hash code bytes into the sink without copying.
 */
public enum HashCodeFunnel implements com.exonum.binding.hash.Funnel<HashCode> {
  INSTANCE;

  @Override
  public void funnel(HashCode from, PrimitiveSink into) {
    into.putBytes(from.getBytesInternal());
  }

  public static com.exonum.binding.hash.Funnel<HashCode> hashCodeFunnel() {
    return HashCodeFunnel.INSTANCE;
  }

}
