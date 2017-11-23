package com.exonum.binding.storage.proofs;

import com.exonum.binding.hash.Funnel;
import com.exonum.binding.hash.PrimitiveSink;
import com.exonum.binding.storage.proofs.map.DbKey;

/**
 * A funnel for a database key. Puts the raw database key (34 bytes) into the sink.
 */
public enum DbKeyFunnel implements Funnel<DbKey> {
  INSTANCE;

  @Override
  public void funnel(DbKey from, PrimitiveSink into) {
    into.putBytes(from.getRawDbKey());
  }

  public static Funnel<DbKey> dbKeyFunnel() {
    return INSTANCE;
  }
}
