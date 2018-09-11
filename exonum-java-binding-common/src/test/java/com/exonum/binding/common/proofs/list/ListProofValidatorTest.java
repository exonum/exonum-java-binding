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

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.hash.Funnel;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.HashFunction;
import com.exonum.binding.common.hash.Hasher;
import com.exonum.binding.common.serialization.StandardSerializers;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ListProofValidatorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String V1 = "v1";
  private static final String V2 = "v2";
  private static final String V3 = "v3";
  private static final String V4 = "v4";

  private static final HashCode H1 = HashCode.fromString("a1");
  private static final HashCode H2 = HashCode.fromString("a2");
  private static final HashCode H3 = HashCode.fromString("a3");
  private static final HashCode EMPTY_HASH = HashCode.fromString("ae");
  private static final HashCode ROOT_HASH = HashCode.fromString("af");

  private Hasher hasher;
  private HashFunction hashFunction;
  private ListProofValidator<String> validator;

  @Before
  @SuppressWarnings("unchecked")
  public void setUp() {
    hasher = mock(Hasher.class);
    when(hasher.putObject(any(), any())).thenReturn(hasher);
    // Return the root hash by default. Individual tests might override that behaviour.
    when(hasher.hash()).thenReturn(ROOT_HASH);

    hashFunction = mock(HashFunction.class);
    when(hashFunction.hashBytes(any(byte[].class))).thenReturn(EMPTY_HASH);
    when(hashFunction.hashObject(any(Object.class), any(Funnel.class)))
        .thenReturn(EMPTY_HASH);
    when(hashFunction.newHasher()).thenReturn(hasher);
  }

  @Test
  public void constructorRejectsZeroSize() {
    expectedException.expect(IllegalArgumentException.class);
    validator = createListProofValidator(0);
  }

  @Test
  public void constructorRejectsNegativeSize() {
    expectedException.expect(IllegalArgumentException.class);
    validator = createListProofValidator(-1);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void visit_SingletonListProof() {
    ListProof root = leafOf(V1);
    when(hashFunction.hashObject(eq(root), any(Funnel.class)))
        .thenReturn(ROOT_HASH);

    int listSize = 1;
    validator = createListProofValidator(listSize);
    root.accept(validator);

    assertTrue(validator.isValid());
    assertThat(validator.getElements(), equalTo(of(0L, V1)));
  }

  @Test
  public void visit_FullProof2elements() {
    ProofListElement left = leafOf(V1);
    when(hashFunction.hashBytes(bytesOf(V1))).thenReturn(H1);

    ProofListElement right = leafOf(V2);
    when(hashFunction.hashBytes(bytesOf(V2))).thenReturn(H2);

    ListProofBranch root = new ListProofBranch(left, right);
    when(hashFunction.newHasher()
        .putObject(eq(H1), any())
        .putObject(eq(Optional.of(H2)), any())
        .hash())
        .thenReturn(ROOT_HASH);

    int listSize = 2;
    validator = createListProofValidator(listSize);
    validator.visit(root);

    assertTrue(validator.isValid());
    assertThat(validator.getElements(), equalTo(of(0L, V1,
        1L, V2)));
  }

  @Test
  public void visit_FullProof4elements() {
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

    when(hasher.hash()).thenReturn(ROOT_HASH);

    int listSize = 4;
    validator = createListProofValidator(listSize);
    validator.visit(root);

    assertTrue(validator.isValid());

    assertThat(validator.getElements(),
        equalTo(of(0L, V1,
            1L, V2,
            2L, V3,
            3L, V4)));
  }

  @Test
  public void visit_FullProof2elementsHashMismatch() {
    ProofListElement left = leafOf(V1);
    ProofListElement right = leafOf(V2);
    ListProofBranch root = new ListProofBranch(left, right);

    when(hasher.hash()).thenReturn(H3); // Just always return H3

    int listSize = 2;
    validator = createListProofValidator(listSize);
    validator.visit(root);

    assertFalse(validator.isValid());

    expectedException.expectMessage("hash mismatch: expected=" + ROOT_HASH
        + ", actual=" + H3);
    expectedException.expect(IllegalStateException.class);
    validator.getElements();
  }

  @Test
  public void visit_IllegalProofOfSingletonTree() {
    int listSize = 1;

    ProofListElement left = leafOf(V1);
    when(hashFunction.hashBytes(bytesOf(V1))).thenReturn(H1);

    // A proof for a list of size 1 must not contain branch nodes.
    ListProofBranch root = new ListProofBranch(left, null);

    validator = createListProofValidator(listSize);
    validator.visit(root);

    assertFalse(validator.isValid());
  }

  @Test
  public void visit_ProofLeftValue() {
    int listSize = 2;

    ListProof left = leafOf(V1);
    when(hashFunction.hashBytes(bytesOf(V1))).thenReturn(H1);

    ListProof right = new HashNode(H2);

    ListProofBranch root = new ListProofBranch(left, right);

    validator = createListProofValidator(listSize);
    validator.visit(root);

    assertTrue(validator.isValid());
    assertThat(validator.getElements(), equalTo(of(0L, V1)));
  }

  @Test
  public void visit_ProofRightValue() {
    int listSize = 2;

    ListProof left = new HashNode(H1);

    ListProof right = leafOf(V2);
    when(hashFunction.hashBytes(bytesOf(V2))).thenReturn(H2);

    ListProofBranch root = new ListProofBranch(left, right);

    validator = createListProofValidator(listSize);
    validator.visit(root);

    assertTrue(validator.isValid());
    assertThat(validator.getElements(), equalTo(of(1L, V2)));
  }

  @Test
  public void visit_InvalidBranchHashesAsChildren() {
    int listSize = 2;

    ListProof left = new HashNode(H1);
    ListProof right = new HashNode(H2);
    ListProofBranch root = new ListProofBranch(left, right);

    validator = createListProofValidator(listSize);
    validator.visit(root);

    assertFalse(validator.isValid());

    expectedException.expect(IllegalStateException.class);
    validator.getElements();
  }

  @Test
  public void visit_InvalidBranchLeftHashNoRight() {
    int listSize = 2;

    ListProof left = new HashNode(H1);
    ListProofBranch root = new ListProofBranch(left, null);

    validator = createListProofValidator(listSize);
    validator.visit(root);

    assertFalse(validator.isValid());

    expectedException.expectMessage("the tree does not contain any value nodes");
    expectedException.expect(IllegalStateException.class);
    validator.getElements();
  }

  @Test
  public void visit_UnbalancedInTheRightSubTree() {
    ListProofBranch root = new ListProofBranch(
        new ListProofBranch(leafOf(V1),
            new HashNode(H2)),
        leafOf(V3) // <-- A value at the wrong depth.
    );

    int listSize = 3;
    validator = createListProofValidator(listSize);
    validator.visit(root);

    assertFalse(validator.isValid());

    expectedException.expectMessage("a value node appears at the wrong level");
    expectedException.expect(IllegalStateException.class);
    validator.getElements();
  }

  @Test
  public void visit_UnbalancedInTheLeftSubTree() {
    ListProofBranch root = new ListProofBranch(
        leafOf(V1), // <-- A value at the wrong depth.
        new ListProofBranch(leafOf(V2),
            new HashNode(H3))
    );

    int listSize = 3;
    validator = createListProofValidator(listSize);
    validator.visit(root);

    assertFalse(validator.isValid());

    expectedException.expectMessage("a value node appears at the wrong level");
    expectedException.expect(IllegalStateException.class);
    validator.getElements();
  }

  @Test
  public void visit_UnbalancedLeafNodeTooDeep() {
    int depth = 4;
    ListProof root = generateLeftLeaningProofTree(depth);

    // A list of size 4 has a height equal to 2, however, the proof tree exceeds that height.
    long listSize = 4;
    validator = createListProofValidator(listSize);

    root.accept(validator);

    assertFalse(validator.isValid());

    expectedException.expectMessage("a value node appears at the wrong level");
    expectedException.expect(IllegalStateException.class);
    validator.getElements();
  }

  private ListProofValidator<String> createListProofValidator(long numElements) {
    return new ListProofValidator<>(ROOT_HASH, numElements, StandardSerializers.string(),
        hashFunction);
  }

  private static ProofListElement leafOf(String element) {
    byte[] dbElement = bytesOf(element);
    return new ProofListElement(dbElement);
  }

  private static byte[] bytesOf(String element) {
    return StandardSerializers.string().toBytes(element);
  }

  private ListProof generateLeftLeaningProofTree(int depth) {
    ListProof root = null;
    ListProof left = leafOf(V1);
    int d = depth;
    while (d != 0) {
      ListProof right = new HashNode(H1);
      root = new ListProofBranch(left, right);
      left = root;
      d--;
    }
    return root;
  }
}
