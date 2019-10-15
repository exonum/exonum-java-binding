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

import static com.exonum.binding.common.proofs.list.ListProofUtils.generateRightLeaningProofTree;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.exonum.binding.common.hash.HashCode;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ListProofStructureValidatorTest {

  private static final ByteString V1 = ByteString.copyFromUtf8("v1");
  private static final ByteString V2 = ByteString.copyFromUtf8("v2");
  private static final ByteString V3 = ByteString.copyFromUtf8("v3");
  private static final ByteString V4 = ByteString.copyFromUtf8("v4");

  private static final HashCode H1 = HashCode.fromString("a1");
  private static final HashCode H2 = HashCode.fromString("a2");
  private static final HashCode H3 = HashCode.fromString("a3");

  private ListProofStructureValidator validator;

  @Test
  void visit_SingletonListProof() {
    ListProofNode root = new ListProofElement(V1);

    validator = new ListProofStructureValidator(root);

    assertTrue(validator.isValid());
  }

  @Test
  void visit_FullProof2elements() {
    ListProofElement left = new ListProofElement(V1);
    ListProofElement right = new ListProofElement(V2);
    ListProofBranch root = new ListProofBranch(left, right);

    validator = new ListProofStructureValidator(root);

    assertTrue(validator.isValid());
  }

  @Test
  void visit_FullProof4elements() {
    ListProofBranch root = new ListProofBranch(
        new ListProofBranch(
            new ListProofElement(V1),
            new ListProofElement(V2)
        ),
        new ListProofBranch(
            new ListProofElement(V3),
            new ListProofElement(V4)
        )
    );

    validator = new ListProofStructureValidator(root);

    assertTrue(validator.isValid());
  }

  @Test
  void visit_ProofLeftValue() {
    ListProofNode left = new ListProofElement(V1);
    ListProofNode right = new ListProofHashNode(H2);
    ListProofBranch root = new ListProofBranch(left, right);

    validator = new ListProofStructureValidator(root);

    assertTrue(validator.isValid());
  }

  @Test
  void visit_ProofRightValue() {
    ListProofNode left = new ListProofHashNode(H1);
    ListProofNode right = new ListProofElement(V2);
    ListProofBranch root = new ListProofBranch(left, right);

    validator = new ListProofStructureValidator(root);

    assertTrue(validator.isValid());
  }

  @Test
  void visit_ProofOfAbsence() {
    ListProofNode root = new ListProofOfAbsence(H1);

    validator = createListProofStructureValidator(root);

    assertTrue(validator.isValid());
  }

  @Test
  void visit_InvalidTreeHasNoElements() {
    ListProofNode left = new ListProofHashNode(H1);
    ListProofBranch root = new ListProofBranch(left, null);

    validator = new ListProofStructureValidator(root);

    assertThat(validator.getProofStatus(), is(ListProofStatus.INVALID_TREE_NO_ELEMENTS));
  }

  @Test
  void visit_UnbalancedInTheRightSubTree() {
    ListProofBranch root = new ListProofBranch(
        new ListProofBranch(new ListProofElement(V1),
            new ListProofHashNode(H2)),
        new ListProofElement(V3) // <-- A value at the wrong depth.
    );

    validator = new ListProofStructureValidator(root);

    assertThat(validator.getProofStatus(), is(ListProofStatus.INVALID_NODE_DEPTH));
  }

  @Test
  void visit_UnbalancedInTheLeftSubTree() {
    ListProofBranch root = new ListProofBranch(
        new ListProofElement(V1), // <-- A value at the wrong depth.
        new ListProofBranch(new ListProofElement(V2),
            new ListProofHashNode(H3))
    );

    validator = new ListProofStructureValidator(root);

    assertThat(validator.getProofStatus(), is(ListProofStatus.INVALID_NODE_DEPTH));
  }

  @Test
  void visit_MaxAllowedDepth() {
    int depth = ListProofStructureValidator.MAX_NODE_DEPTH;
    ListProofNode root = generateRightLeaningProofTree(depth, new ListProofElement(V1));

    validator = new ListProofStructureValidator(root);

    assertTrue(validator.isValid());
  }

  @Test
  void visit_UnbalancedElementNodeTooDeep() {
    int depth = ListProofStructureValidator.MAX_NODE_DEPTH + 1;
    ListProofNode root = generateRightLeaningProofTree(depth, new ListProofElement(V1));

    validator = new ListProofStructureValidator(root);

    assertThat(validator.getProofStatus(), is(ListProofStatus.INVALID_ELEMENT_NODE_DEPTH));
  }

  @Test
  void visit_UnbalancedHashNodeTooDeep() {
    int depth = ListProofStructureValidator.MAX_NODE_DEPTH + 1;
    ListProofNode root = generateRightLeaningProofTree(depth, new ListProofHashNode(H2));

    validator = new ListProofStructureValidator(root);

    assertThat(validator.getProofStatus(), is(ListProofStatus.INVALID_HASH_NODE_DEPTH));
  }

  @Test
  void visit_UnbalancedHashNodesOnlyLeafs() {
    ListProofBranch root = new ListProofBranch(
        new ListProofBranch(
            new ListProofElement(V1),
            new ListProofHashNode(H1)
        ),
        new ListProofBranch(
            new ListProofHashNode(H2), // <-- left leaf is hash node
            new ListProofHashNode(H3)  // <-- right leaf is hash node
        )
    );

    validator = new ListProofStructureValidator(root);

    assertThat(validator.getProofStatus(), is(ListProofStatus.INVALID_HASH_NODES_COUNT));
  }

  @Test
  void visit_UnbalancedBranchHasOnlyHashNode() {
    ListProofBranch root = new ListProofBranch(
        new ListProofBranch(
            new ListProofElement(V1),
            new ListProofHashNode(H1)
        ),
        new ListProofBranch(
            new ListProofHashNode(H2), // <-- left leaf is hash node
            null                 // <-- no right leaf
        )
    );

    validator = new ListProofStructureValidator(root);

    assertThat(validator.getProofStatus(), is(ListProofStatus.INVALID_HASH_NODES_COUNT));
  }

  @Test
  @Disabled // Such check is not implemented yet: ECR-2490
  void visit_InvalidBranchHasMissingRightElement() {
    ListProofBranch root = new ListProofBranch(
        new ListProofBranch(
            new ListProofElement(V1),
            null // Having no child is not allowed here for a tree of height 2.
        ),
        new ListProofHashNode(H1)
    );

    validator = new ListProofStructureValidator(root);

    assertFalse(validator.isValid());
  }

  @Test
  void visit_InvalidProofOfAbsence() {
    ListProofBranch root = new ListProofBranch(
        new ListProofBranch(
            new ListProofElement(V1),
            // Having proof of absence not as a proof tree root is not allowed
            new ListProofOfAbsence(H1)
        ),
        new ListProofHashNode(H2)
    );

    validator = createListProofStructureValidator(root);

    assertThat(validator.getProofStatus(), is(ListProofStatus.INVALID_PROOF_OF_ABSENCE));
  }

  private ListProofStructureValidator createListProofStructureValidator(ListProofNode listProof) {
    return new ListProofStructureValidator(listProof);
  }
}
