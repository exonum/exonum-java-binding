package com.exonum.binding.storage.proofs;

import static com.exonum.binding.test.Bytes.bytes;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    Hashes.class
})
public class ListProofValidatorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final byte[] V1 = bytes("v1");
  private static final byte[] V2 = bytes("v2");
  private static final byte[] V3 = bytes("v3");
  private static final byte[] V4 = bytes("v4");

  private static final byte[] H1 = bytes("h1");
  private static final byte[] H2 = bytes("h2");
  private static final byte[] H3 = bytes("h3");
  private static final byte[] EMPTY_HASH = new byte[0];
  private static final byte[] ROOT_HASH = bytes("h4");

  private ListProofValidator validator;

  @Before
  public void setUp() throws Exception {
    mockStatic(Hashes.class);
  }

  @Test
  public void visitFullProof_2elements() throws Exception {
    ProofListElement left = new ProofListElement(V1);
    when(Hashes.getHashOf(V1)).thenReturn(H1);

    ProofListElement right = new ProofListElement(V2);
    when(Hashes.getHashOf(V2)).thenReturn(H2);

    ListProofBranch root = new ListProofBranch(left, right);
    when(Hashes.getHashOf(H1, H2)).thenReturn(ROOT_HASH);

    int listSize = 2;
    validator = new ListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertTrue(validator.isValid());
    assertThat(validator.getElements())
        .containsExactly(
            0L, V1,
            1L, V2);
  }

  @Test
  public void visitFullProof_4elements() throws Exception {
    ListProofBranch root = new ListProofBranch(
        new ListProofBranch(
            new ProofListElement(V1),
            new ProofListElement(V2)
        ),
        new ListProofBranch(
            new ProofListElement(V3),
            new ProofListElement(V4)
        )
    );

    when(Hashes.getHashOf(any())).thenReturn(ROOT_HASH);

    int listSize = 4;
    validator = new ListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertTrue(validator.isValid());
    assertThat(validator.getElements())
        .containsExactly(
            0L, V1,
            1L, V2,
            2L, V3,
            3L, V4);
  }

  @Test
  public void visitFullProofHashMismatch() throws Exception {
    ProofListElement left = new ProofListElement(V1);
    when(Hashes.getHashOf(V1)).thenReturn(H1);

    ProofListElement right = new ProofListElement(V2);
    when(Hashes.getHashOf(V2)).thenReturn(H2);

    ListProofBranch root = new ListProofBranch(left, right);
    when(Hashes.getHashOf(H1, H2)).thenReturn(H3);

    int listSize = 2;
    validator = new ListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertFalse(validator.isValid());

    expectedException.expectMessage("hash mismatch: expected=" + Hashes.toString(ROOT_HASH)
        + ", actual=" + Hashes.toString(H3));
    expectedException.expect(IllegalStateException.class);
    validator.getElements();
  }

  @Test
  public void visitFullProofNoRight() throws Exception {
    int listSize = 1;

    ProofListElement left = new ProofListElement(V1);
    when(Hashes.getHashOf(V1)).thenReturn(H1);

    ListProofBranch root = new ListProofBranch(left, null);
    when(Hashes.getHashOf(eq(H1), eq(EMPTY_HASH))).thenReturn(ROOT_HASH);

    validator = new ListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertTrue(validator.isValid());
    assertThat(validator.getElements())
        .containsExactly(0L, V1);
  }

  @Test
  public void visitProofLeft() throws Exception {
    int listSize = 2;

    ListProof left = new ProofListElement(V1);
    when(Hashes.getHashOf(V1)).thenReturn(H1);

    ListProof right = new HashNode(H2);

    ListProofBranch root = new ListProofBranch(left, right);
    when(Hashes.getHashOf(eq(H1), eq(H2))).thenReturn(ROOT_HASH);

    validator = new ListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertTrue(validator.isValid());
    assertThat(validator.getElements())
        .containsExactly(0L, V1);
  }

  @Test
  public void visitProofRight() throws Exception {
    int listSize = 2;

    ListProof left = new HashNode(H1);

    ListProof right = new ProofListElement(V2);
    when(Hashes.getHashOf(V2)).thenReturn(H2);

    ListProofBranch root = new ListProofBranch(left, right);
    when(Hashes.getHashOf(eq(H1), eq(H2))).thenReturn(ROOT_HASH);

    validator = new ListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertTrue(validator.isValid());
    assertThat(validator.getElements())
        .containsExactly(1L, V2);
  }

  @Test
  public void visitBothHashes() throws Exception {
    int listSize = 2;

    ListProof left = new HashNode(H1);
    ListProof right = new HashNode(H2);
    ListProofBranch root = new ListProofBranch(left, right);
    when(Hashes.getHashOf(eq(H1), eq(H2))).thenReturn(ROOT_HASH);

    validator = new ListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertFalse(validator.isValid());

    expectedException.expect(IllegalStateException.class);
    validator.getElements();
  }

  @Test
  public void visitLeftHash() throws Exception {
    int listSize = 1;

    ListProof left = new HashNode(H1);
    ListProofBranch root = new ListProofBranch(left, null);
    when(Hashes.getHashOf(eq(H1), eq(EMPTY_HASH))).thenReturn(ROOT_HASH);

    validator = new ListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertFalse(validator.isValid());

    expectedException.expectMessage("the tree does not contain any value nodes");
    expectedException.expect(IllegalStateException.class);
    validator.getElements();
  }

  @Test
  public void visitUnbalancedInTheRightSubTree() throws Exception {
    ListProofBranch root = new ListProofBranch(
        new ListProofBranch(new ProofListElement(V1),
            new HashNode(H2)),
        new ProofListElement(V3) // <-- A value at the wrong depth.
    );

    int listSize = 3;
    validator = new ListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertFalse(validator.isValid());

    expectedException.expectMessage("a value node appears at the wrong level");
    expectedException.expect(IllegalStateException.class);
    validator.getElements();
  }

  @Test
  public void visitUnbalancedInTheLeftSubTree() throws Exception {
    ListProofBranch root = new ListProofBranch(
        new ProofListElement(V1), // <-- A value at the wrong depth.
        new ListProofBranch(new ProofListElement(V2),
            new HashNode(H3))
    );

    int listSize = 3;
    validator = new ListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertFalse(validator.isValid());

    expectedException.expectMessage("a value node appears at the wrong level");
    expectedException.expect(IllegalStateException.class);
    validator.getElements();
  }

  @Test
  public void visitUnbalancedLeafNodeTooDeep() throws Exception {
    int depth = 4;
    ListProof root = generateLeftLeaningProofTree(depth);

    // A list of size 4 has a height equal to 2, however, the proof tree exceeds that height.
    long listSize = 4;
    validator = new ListProofValidator(ROOT_HASH, listSize);
    when(Hashes.getHashOf(any())).thenReturn(ROOT_HASH);

    root.accept(validator);

    assertFalse(validator.isValid());

    expectedException.expectMessage("a value node appears at the wrong level");
    expectedException.expect(IllegalStateException.class);
    validator.getElements();
  }

  private ListProof generateLeftLeaningProofTree(int depth) {
    ListProof root = null;
    ListProof left = new ProofListElement(V1);
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
