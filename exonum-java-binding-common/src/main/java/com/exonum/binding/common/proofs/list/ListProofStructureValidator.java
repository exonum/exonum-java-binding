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
import java.util.Arrays;
import java.util.List;

/**
 * A validator that checks list proofs internal structure.
 */
public final class ListProofStructureValidator implements ListProofVisitor {

  static int MAX_NODE_DEPTH = 63;

  //TODO optimize data storage and algorithms of validity checks, https://jira.bf.local/browse/ECR-2443.
  private List<NodeInfo> listProofBranchesInfo;

  private List<NodeInfo> listProofElementsInfo;

  private List<NodeInfo> listProofHashNodesInfo;

  private int depth;

  private ProofStatus proofStatus;

  /**
   * Creates a new ListProofStructureValidator.
   */
  public ListProofStructureValidator() {
    depth = 0;
    proofStatus = ListProofStatus.VALID;
    listProofBranchesInfo = new ArrayList<>();
    listProofElementsInfo = new ArrayList<>();
    listProofHashNodesInfo = new ArrayList<>();
  }

  @Override
  public void visit(ListProofBranch branch) {
    int branchDepth = depth;

    NodeType leftElementType = branch.getLeft().getNodeType();
    NodeType rightElementType = branch.getRight()
        .map(ListProofNode::getNodeType)
        .orElse(NodeType.NONE);
    listProofBranchesInfo.add(
        new NodeInfo(branch, depth, Arrays.asList(leftElementType, rightElementType))
    );

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

    if (hashNodesLimitExceeded(listProofBranchesInfo)) {
      proofStatus = ListProofStatus.INVALID_HASH_NODES_COUNT;
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
        .anyMatch(nodeInfo -> nodeInfo.getDepth() > MAX_NODE_DEPTH);
  }

  /**
   * Returns true if nodes appear at different depths.
   *
   * @param nodes collection of node info
   * @return true if node depths vary
   */
  private boolean hasInvalidNodesDepth(List<NodeInfo> nodes) {
    long depthsCount = nodes.stream()
        .map(NodeInfo::getDepth)
        .distinct()
        .count();
    return depthsCount > 1;
  }

  /**
   * Returns true if tree doesn't contain listProofElement nodes.
   *
   * @param branches collection of branches info
   * @param nodes collection of node info
   * @return true if node depths vary
   */
  private boolean hasNoElementNodes(List<NodeInfo> branches, List<NodeInfo> nodes) {
    return branches.size() > 0 && nodes.size() == 0;
  }

  /**
   * Returns true if branch node contains 2 hash nodes.
   *
   * @param branches collection of branches info
   * @return true if branch contains only hash nodes.
   */
  private boolean hashNodesLimitExceeded(List<NodeInfo> branches) {
    return branches.stream()
        .anyMatch(
            branch -> branch.getChildElementsTypes().size() == 2
                && branch.getChildElementsTypes().stream()
                .allMatch(nodeType -> nodeType.equals(NodeType.HASHNODE))
        );
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
    private ListProofNode node;
    private int depth;
    private List<NodeType> childElementsTypes;

    NodeInfo(ListProofNode node, int depth) {
      this.node = node;
      this.depth = depth;
    }

    NodeInfo(ListProofNode node, int depth, List<NodeType> childElementsTypes) {
      this(node, depth);
      this.childElementsTypes = childElementsTypes;
    }

    ListProofNode getNode() {
      return node;
    }

    int getDepth() {
      return depth;
    }

    List<NodeType> getChildElementsTypes() {
      return childElementsTypes;
    }
  }

  /**
   * Enum used to identify tree node type.
   */
  enum NodeType {
    BRANCH, ELEMENT, HASHNODE, NONE
  }
}

