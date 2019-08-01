package com.exonum.binding.common.proofs.list;

import static com.google.common.base.Preconditions.checkNotNull;

// TODO: add Javadocs
public class ListProofRoot {

  private final ListProofNode rootNode;

  private final long length;

  public ListProofRoot(ListProofNode rootNode, long length) {
    this.rootNode = checkNotNull(rootNode);
    this.length = length;
  }

  public ListProofNode getRootNode() {
    return rootNode;
  }

  public long getLength() {
    return length;
  }
}
