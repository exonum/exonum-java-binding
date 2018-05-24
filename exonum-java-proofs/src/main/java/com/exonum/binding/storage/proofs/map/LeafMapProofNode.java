package com.exonum.binding.storage.proofs.map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A proof node for a map that contains a mapping for the requested key.
 */
public final class LeafMapProofNode implements MapProofNode {

  private final byte[] value;

  public LeafMapProofNode(byte[] value) {
    this.value = checkNotNull(value);
  }

  public byte[] getValue() {
    return value;
  }

  @Override
  public void accept(MapProofVisitor visitor) {
    visitor.visit(this);
  }
}
