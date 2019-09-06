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

import static com.exonum.binding.common.proofs.list.ListProofUtils.getBranchHashCode;
import static com.exonum.binding.common.proofs.list.ListProofUtils.getNodeHashCode;
import static com.exonum.binding.common.proofs.list.ListProofUtils.getProofListHash;
import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.exonum.binding.common.hash.HashCode;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;

class ListProofHashCalculatorTest {

  private static final ByteString V1 = ByteString.copyFromUtf8("v1");
  private static final ByteString V2 = ByteString.copyFromUtf8("v2");
  private static final ByteString V3 = ByteString.copyFromUtf8("v3");
  private static final ByteString V4 = ByteString.copyFromUtf8("v4");

  private static final HashCode H1 = HashCode.fromString("a1");
  private static final HashCode H2 = HashCode.fromString("a2");

  private ListProofHashCalculator calculator;

  @Test
  void visit_SingletonListProof() {
    ListProofNode root = new ListProofElement(V1);

    long length = 1;
    calculator = new ListProofHashCalculator(root, length);

    HashCode expectedProofListHash = getProofListHash(getNodeHashCode(V1), length);

    assertThat(calculator.getElements(), equalTo(of(0L, V1)));
    assertEquals(expectedProofListHash, calculator.getHash());
  }

  @Test
  void visit_FullProof2elements() {
    ListProofElement left = new ListProofElement(V1);
    ListProofElement right = new ListProofElement(V2);
    ListProofBranch root = new ListProofBranch(left, right);

    long length = 2;
    calculator = new ListProofHashCalculator(root, length);

    // Calculate expected proof list hash
    HashCode expectedRootHash = getBranchHashCode(getNodeHashCode(V1), getNodeHashCode(V2));
    HashCode expectedProofListHash = getProofListHash(expectedRootHash, length);

    assertThat(calculator.getElements(), equalTo(of(0L, V1,
        1L, V2)));
    assertEquals(expectedProofListHash, calculator.getHash());
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

    long length = 4;
    calculator = new ListProofHashCalculator(root, length);

    // Calculate expected proof list hash
    HashCode leftBranchHash = getBranchHashCode(getNodeHashCode(V1), getNodeHashCode(V2));
    HashCode rightBranchHash = getBranchHashCode(getNodeHashCode(V3), getNodeHashCode(V4));
    HashCode expectedRootHash = getBranchHashCode(leftBranchHash, rightBranchHash);
    HashCode expectedProofListHash = getProofListHash(expectedRootHash, length);


    assertThat(calculator.getElements(),
        equalTo(of(0L, V1,
            1L, V2,
            2L, V3,
            3L, V4)));
    assertEquals(expectedProofListHash, calculator.getHash());
  }

  @Test
  void visit_ProofLeftValue() {
    ListProofNode left = new ListProofElement(V1);
    ListProofNode right = new ListProofHashNode(H2);
    ListProofBranch root = new ListProofBranch(left, right);

    long length = 2;
    calculator = new ListProofHashCalculator(root, length);

    HashCode expectedRootHash = getBranchHashCode(getNodeHashCode(V1), H2);
    HashCode expectedProofListHash = getProofListHash(expectedRootHash, length);

    assertThat(calculator.getElements(), equalTo(of(0L, V1)));
    assertEquals(expectedProofListHash, calculator.getHash());
  }

  @Test
  void visit_ProofRightValue() {
    ListProofNode left = new ListProofHashNode(H1);
    ListProofNode right = new ListProofElement(V2);
    ListProofBranch root = new ListProofBranch(left, right);

    long length = 2;
    calculator = new ListProofHashCalculator(root, length);

    HashCode expectedRootHash = getBranchHashCode(H1, getNodeHashCode(V2));
    HashCode expectedProofListHash = getProofListHash(expectedRootHash, length);

    assertThat(calculator.getElements(), equalTo(of(1L, V2)));
    assertEquals(expectedProofListHash, calculator.getHash());
  }

  @Test
  void visit_FullProof3elements() {
    ListProofBranch root = new ListProofBranch(
        new ListProofBranch(
            new ListProofElement(V1),
            new ListProofElement(V2)
        ),
        new ListProofBranch(
            new ListProofElement(V3),
            null
        )
    );

    long length = 3;
    calculator = new ListProofHashCalculator(root, length);

    HashCode leftBranchHash = getBranchHashCode(getNodeHashCode(V1), getNodeHashCode(V2));
    HashCode rightBranchHash = getBranchHashCode(getNodeHashCode(V3), null);
    HashCode expectedRootHash = getBranchHashCode(leftBranchHash, rightBranchHash);
    HashCode expectedProofListHash = getProofListHash(expectedRootHash, length);

    assertThat(calculator.getElements(),
        equalTo(of(
            0L, V1,
            1L, V2,
            2L, V3))
    );
    assertEquals(expectedProofListHash, calculator.getHash());
  }
}
