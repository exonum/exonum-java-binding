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

package com.exonum.binding.common.proofs.list;

import com.exonum.binding.common.proofs.common.ProofStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A validator that checks list proofs internal structure.
 */
public final class ListProofStructureValidator implements ListProofVisitor {

  public static int MAX_NODE_DEPTH = 64;

  private List<NodeInfo> listProofBranchesInfo;

  private List<NodeInfo> listProofElementsInfo;

  private List<NodeInfo> listProofHashNodesInfo;

  private int depth;

  private ProofStatus proofStatus;

  ListProofStructureValidator() {
    depth = 0;
    proofStatus = ListProofStatus.VALID;
    listProofBranchesInfo = new ArrayList<>();
    listProofElementsInfo = new ArrayList<>();
    listProofHashNodesInfo = new ArrayList<>();
  }

  @Override
  public void visit(ListProofBranch branch) {
    int branchDepth = depth;

    listProofBranchesInfo.add(new NodeInfo(branch, depth));
    visitLeft(branch, branchDepth);
    visitRight(branch, branchDepth);
  }

  @Override
  public void visit(ListProofHashNode hashNode) {
    listProofHashNodesInfo.add(new NodeInfo(hashNode, depth));
  }

  @Override
  public void visit(ListProofElement element) {
    listProofElementsInfo.add(new NodeInfo(element, depth));
  }

  private void visitLeft(ListProofBranch branch, int branchDepth) {
    depth = getChildDepth(branchDepth);
    branch.getLeft().accept(this);
  }

  private void visitRight(ListProofBranch branch, int branchDepth) {
    depth = getChildDepth(branchDepth);
    branch.getRight().ifPresent((right) -> right.accept(this));
  }

  private int getChildDepth(int branchDepth) {
    return branchDepth + 1;
  }

  /**
   * Returns list proof check status.
   */
  public ProofStatus check() {
    if (exceedsMaxDepth(listProofElementsInfo)) {
      proofStatus = ListProofStatus.INVALID_ELEMENT_NODE_DEPTH;
      return proofStatus;
    }

    if (exceedsMaxDepth(listProofHashNodesInfo)) {
      proofStatus = ListProofStatus.INVALID_HASH_NODE_DEPTH;
      return proofStatus;
    }

    if (hasInvalidNodesDepth(listProofElementsInfo)) {
      proofStatus = ListProofStatus.INVALID_NODE_DEPTH;
      return proofStatus;
    }

    if (hasNoElementNodes(listProofBranchesInfo, listProofElementsInfo)) {
      proofStatus = ListProofStatus.INVALID_TREE_NO_ELEMENTS;
      return proofStatus;
    }

    return proofStatus;
  }

  /**
   * Returns true if node exceeds the maximum depth at which nodes may appear.
   *
   * @param nodes collection of node info
   * @return true if node depth is invalid
   */
  private boolean exceedsMaxDepth(List<NodeInfo> nodes) {
    return nodes.stream()
        .anyMatch(nodeInfo -> nodeInfo.getDepth() >= MAX_NODE_DEPTH);
  }

  /**
   * Returns true if nodes has different depths.
   *
   * @param nodes collection of node info
   * @return true if node depths vary
   */
  private boolean hasInvalidNodesDepth(List<NodeInfo> nodes) {
    Map<Integer, List<NodeInfo>> groupByDepth = nodes.stream()
        .collect(Collectors.groupingBy(NodeInfo::getDepth));
    return groupByDepth.size() > 1;
  }

  /**
   * Returns true if tree doesn't contain listProofElement nodes.
   *
   * @param branches collection of branches info
   * @param nodes    collection of node info
   * @return true if node depths vary
   */
  private boolean hasNoElementNodes(List<NodeInfo> branches, List<NodeInfo> nodes) {
    return branches.size() > 0 && nodes.size() == 0;
  }

  /**
   * Returns proof status.
   *
   * @return proof status
   */
  public ProofStatus getProofStatus() {
    return proofStatus;
  }

  @Override
  public String toString() {
    return "ListProofStructureValidator{"
        + ", proofStatus" + proofStatus
        + ", depth=" + depth
        + '}';
  }

  /**
   * Class used to store node info additional information.
   */
  static class NodeInfo {
    private ListProof node;
    private int depth;

    NodeInfo(ListProof node, int depth) {
      this.node = node;
      this.depth = depth;
    }

    public ListProof getNode() {
      return node;
    }

    public int getDepth() {
      return depth;
    }
  }
}

