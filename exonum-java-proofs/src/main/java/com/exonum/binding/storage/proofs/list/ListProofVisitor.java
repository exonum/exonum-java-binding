package com.exonum.binding.storage.proofs.list;

public interface ListProofVisitor {

  void visit(ListProofBranch branch);

  void visit(HashNode hashNode);

  void visit(ProofListElement value);
}
