package com.exonum.binding.storage.proofs.map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A proof node for a singleton proof map, that contains mapping for the specified key.
 */
public class EqualValueAtRoot implements MapProof {

  private final DbKey databaseKey;

  private final byte[] value;

  /**
   * Creates a new proof node.
   *
   * @param databaseKey a database key
   * @param value a value mapped to the key
   */
  public EqualValueAtRoot(DbKey databaseKey, byte[] value) {
    this.databaseKey = checkNotNull(databaseKey);
    this.value = checkNotNull(value);
  }

  public DbKey getDatabaseKey() {
    return databaseKey;
  }

  public byte[] getValue() {
    return value;
  }

  @Override
  public void accept(MapProofVisitor visitor) {
    visitor.visit(this);
  }
}
