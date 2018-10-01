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

import static com.exonum.binding.common.hash.Funnels.hashCodeFunnel;
import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.hash.PrimitiveSink;
import com.exonum.binding.common.serialization.StandardSerializers;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;

class ListProofHashCodeCalculatorTest {

  private static final String V1 = "v1";
  private static final String V2 = "v2";
  private static final String V3 = "v3";
  private static final String V4 = "v4";

  private static final HashCode H1 = HashCode.fromString("a1");
  private static final HashCode H2 = HashCode.fromString("a2");

  private ListProofRootHashCalculator<String> calculator;

  @Test
  void visit_SingletonListProof() {
    ListProofNode root = leafOf(V1);

    calculator = createListProofCalculator();
    root.accept(calculator);

    assertThat(calculator.getElements(), equalTo(of(0L, V1)));
    assertEquals(getNodeHashCode(V1), calculator.getCalculatedRootHash());
  }

  @Test
  void visit_FullProof2elements() {
    ListProofElement left = leafOf(V1);
    ListProofElement right = leafOf(V2);
    ListProofBranch root = new ListProofBranch(left, right);

    calculator = createListProofCalculator();
    calculator.visit(root);

    //calculate expected root hash
    HashCode expectedRootHash = getBranchHashCode(getNodeHashCode(V1), getNodeHashCode(V2));

    assertThat(calculator.getElements(), equalTo(of(0L, V1,
        1L, V2)));
    assertEquals(expectedRootHash, calculator.getCalculatedRootHash());
  }

  @Test
  void visit_FullProof4elements() {
    ListProofBranch root = new ListProofBranch(
        new ListProofBranch(
            leafOf(V1),
            leafOf(V2)
        ),
        new ListProofBranch(
            leafOf(V3),
            leafOf(V4)
        )
    );

    //calculate expected root hash
    HashCode leftBranchHash = getBranchHashCode(getNodeHashCode(V1), getNodeHashCode(V2));
    HashCode rightBranchHash = getBranchHashCode(getNodeHashCode(V3), getNodeHashCode(V4));
    HashCode expectedRootHash = getBranchHashCode(leftBranchHash, rightBranchHash);

    calculator = createListProofCalculator();
    calculator.visit(root);

    assertThat(calculator.getElements(),
        equalTo(of(0L, V1,
            1L, V2,
            2L, V3,
            3L, V4)));
    assertEquals(expectedRootHash, calculator.getCalculatedRootHash());
  }

  @Test
  void visit_FullProof2elementsHashMismatch() {
    ListProofElement left = leafOf(V1);
    ListProofElement right = leafOf(V2);
    ListProofBranch root = new ListProofBranch(left, right);

    HashCode expectedRootHash = getBranchHashCode(getNodeHashCode(V1), getNodeHashCode(V2));

    calculator = createListProofCalculatorSha256HashingFunction();
    calculator.visit(root);

    assertNotEquals(expectedRootHash, calculator.getCalculatedRootHash());
  }

  @Test
  void visit_ProofLeftValue() {
    ListProofNode left = leafOf(V1);
    ListProofNode right = new ListProofHashNode(H2);
    ListProofBranch root = new ListProofBranch(left, right);

    HashCode expectedRootHash = getBranchHashCode(getNodeHashCode(V1), H2);

    calculator = createListProofCalculator();
    calculator.visit(root);

    assertThat(calculator.getElements(), equalTo(of(0L, V1)));
    assertEquals(expectedRootHash, calculator.getCalculatedRootHash());
  }

  @Test
  void visit_ProofRightValue() {
    ListProofNode left = new ListProofHashNode(H1);
    ListProofNode right = leafOf(V2);
    ListProofBranch root = new ListProofBranch(left, right);

    calculator = createListProofCalculator();
    calculator.visit(root);

    HashCode expectedRootHash = getBranchHashCode(H1, getNodeHashCode(V2));

    assertThat(calculator.getElements(), equalTo(of(1L, V2)));
    assertEquals(expectedRootHash, calculator.getCalculatedRootHash());
  }

  private HashCode getNodeHashCode(String v1) {
    return Hashing.defaultHashFunction().newHasher()
        .putString(v1, Charset.defaultCharset())
        .hash();
  }

  private HashCode getBranchHashCode(HashCode leftHash, HashCode rightHash) {
    return Hashing.defaultHashFunction().newHasher()
        .putObject(leftHash, hashCodeFunnel())
        .putObject(rightHash, (HashCode from, PrimitiveSink into) ->
            hashCodeFunnel().funnel(from, into))
        .hash();
  }

  private ListProofRootHashCalculator<String> createListProofCalculator() {
    return new ListProofRootHashCalculator<>(StandardSerializers.string());
  }

  private ListProofRootHashCalculator<String> createListProofCalculatorSha256HashingFunction() {
    return new ListProofRootHashCalculator<>(StandardSerializers.string(), Hashing.sha512());
  }

  private static ListProofElement leafOf(String element) {
    byte[] dbElement = bytesOf(element);
    return new ListProofElement(dbElement);
  }

  private static byte[] bytesOf(String element) {
    return StandardSerializers.string().toBytes(element);
  }
}
