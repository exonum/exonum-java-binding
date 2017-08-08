package com.exonum.binding.storage.proofs;

public interface ListProofVisitor {

  void visit(ListProofBranch branch);

  void visit(HashNode hashNode);

  void visit(ProofListElement value);
}
