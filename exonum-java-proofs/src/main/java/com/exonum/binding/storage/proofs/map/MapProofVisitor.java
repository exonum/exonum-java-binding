package com.exonum.binding.storage.proofs.map;

public interface MapProofVisitor {

  void visit(EqualValueAtRoot equalValueAtRoot);

  void visit(NonEqualValueAtRoot nonEqualValueAtRoot);

  void visit(EmptyMapProof emptyMap);

  void visit(LeftMapProofBranch leftMapProofBranch);

  void visit(RightMapProofBranch rightMapProofBranch);

  void visit(MappingNotFoundProofBranch mappingNotFoundProofBranch);

  void visit(LeafMapProofNode leafMapProofNode);
}
