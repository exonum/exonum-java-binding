/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.common.proofs.map;

/**
 * A visitor of map proofs, which prints each node starting with a root node.
 */
public final class MapProofTreePrinter implements MapProofVisitor {

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
