package com.exonum.binding.storage.proofs.map;

/**
 * A visitor of map proofs, which prints each node starting with a root node.
 */
public class MapProofTreePrinter implements MapProofVisitor {

  private static final int INDENT = 2;

  private int offset;

  public MapProofTreePrinter() {
    offset = 0;
  }

  @Override
  public void visit(EqualValueAtRoot node) {
    System.out.println(indent() + node);
  }

  @Override
  public void visit(NonEqualValueAtRoot node) {
    System.out.println(indent() + node);
  }

  @Override
  public void visit(EmptyMapProof node) {
    System.out.println(node);
  }

  @Override
  public void visit(LeftMapProofBranch node) {
    System.out.println(indent() + node);
    scopedVisit(() -> {
      node.getLeft().accept(this);
    });
  }

  @Override
  public void visit(RightMapProofBranch node) {
    System.out.println(indent() + node);
    scopedVisit(() -> {
      node.getRight().accept(this);
    });
  }

  @Override
  public void visit(MappingNotFoundProofBranch node) {
    System.out.println(indent() + node);
  }

  @Override
  public void visit(LeafMapProofNode node) {
    System.out.println(indent() + node);
  }

  private void scopedVisit(Runnable r) {
    offset += INDENT;
    r.run();
    offset -= INDENT;
  }

  private String indent() {
    StringBuilder sb = new StringBuilder(offset);
    for (int i = 0; i < offset; i++) {
      sb.append(' ');
    }
    return sb.toString();
  }
}
