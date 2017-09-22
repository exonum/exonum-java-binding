package com.exonum.binding.storage.proofs.map;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.hash.HashCode;

/**
 * A proof node for a map that does not contain a mapping for the requested key.
 */
public class NonEqualValueAtRoot implements MapProof {

  private final DbKey databaseKey;

  private final HashCode valueHash;

  /**
   * Create a new proof node.
   *
   * @param databaseKey a database key
   * @param valueHash a hash of the value mapped to the key
   */
  public NonEqualValueAtRoot(DbKey databaseKey, HashCode valueHash) {
    this.databaseKey = checkNotNull(databaseKey);
    this.valueHash = checkNotNull(valueHash);
  }

  public DbKey getDatabaseKey() {
    return databaseKey;
  }

  public HashCode getValueHash() {
    return valueHash;
  }

  @Override
  public void accept(MapProofVisitor visitor) {
    visitor.visit(this);
  }
}
