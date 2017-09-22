package com.exonum.binding.storage.proofs.list;

import static com.exonum.binding.test.Bytes.bytes;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ListProofValidatorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final byte[] V1 = bytes("v1");
  private static final byte[] V2 = bytes("v2");
  private static final byte[] V3 = bytes("v3");
  private static final byte[] V4 = bytes("v4");

  private static final HashCode H1 = HashCode.fromString("a1");
  private static final HashCode H2 = HashCode.fromString("a2");
  private static final HashCode H3 = HashCode.fromString("a3");
  private static final HashCode EMPTY_HASH = HashCode.fromString("ae");
  private static final HashCode ROOT_HASH = HashCode.fromString("af");

  private Hasher hasher;
  private HashFunction hashFunction;
  private ListProofValidator validator;

  @Before
  public void setUp() throws Exception {
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
  public void constructorRejectsZeroSize() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    validator = createListProofValidator(ROOT_HASH, 0);
  }

  @Test
  public void constructorRejectsNegativeSize() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    validator = createListProofValidator(ROOT_HASH, -1);
  }

  @Test
  public void visit_SingletonListProof() throws Exception {
    ListProof root = new ProofListElement(V1);
    when(hashFunction.hashObject(eq(root), any(Funnel.class)))
        .thenReturn(ROOT_HASH);

    int listSize = 1;
    validator = createListProofValidator(ROOT_HASH, listSize);
    root.accept(validator);

    assertTrue(validator.isValid());
    assertThat(validator.getElements(), containsExactly(of(0L, V1)));
  }

  @Test
  public void visit_FullProof2elements() throws Exception {
    ProofListElement left = new ProofListElement(V1);
    when(hashFunction.hashBytes(V1)).thenReturn(H1);

    ProofListElement right = new ProofListElement(V2);
    when(hashFunction.hashBytes(V2)).thenReturn(H2);

    ListProofBranch root = new ListProofBranch(left, right);
    when(hashFunction.newHasher()
        .putObject(eq(H1), any())
        .putObject(eq(Optional.of(H2)), any())
        .hash())
        .thenReturn(ROOT_HASH);

    int listSize = 2;
    validator = createListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertTrue(validator.isValid());
    Map<Long, byte[]> elements = validator.getElements();
    assertThat(elements, containsExactly(of(0L, V1,
        1L, V2)));
  }

  @Test
  public void visit_FullProof4elements() throws Exception {
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

    when(hasher.hash()).thenReturn(ROOT_HASH);

    int listSize = 4;
    validator = createListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertTrue(validator.isValid());

    assertThat(validator.getElements(),
        containsExactly(of(0L, V1,
            1L, V2,
            2L, V3,
            3L, V4)));
  }

  @Test
  public void visit_FullProof2elementsHashMismatch() throws Exception {
    ProofListElement left = new ProofListElement(V1);
    ProofListElement right = new ProofListElement(V2);
    ListProofBranch root = new ListProofBranch(left, right);

    when(hasher.hash()).thenReturn(H3); // Just always return H3

    int listSize = 2;
    validator = createListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertFalse(validator.isValid());

    expectedException.expectMessage("hash mismatch: expected=" + ROOT_HASH
        + ", actual=" + H3);
    expectedException.expect(IllegalStateException.class);
    validator.getElements();
  }

  @Test
  public void visit_IllegalProofOfSingletonTree() throws Exception {
    int listSize = 1;

    ProofListElement left = new ProofListElement(V1);
    when(hashFunction.hashBytes(V1)).thenReturn(H1);

    // A proof for a list of size 1 must not contain branch nodes.
    ListProofBranch root = new ListProofBranch(left, null);

    validator = createListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertFalse(validator.isValid());
  }

  @Test
  public void visit_ProofLeftValue() throws Exception {
    int listSize = 2;

    ListProof left = new ProofListElement(V1);
    when(hashFunction.hashBytes(V1)).thenReturn(H1);

    ListProof right = new HashNode(H2);

    ListProofBranch root = new ListProofBranch(left, right);

    validator = createListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertTrue(validator.isValid());
    assertThat(validator.getElements(), containsExactly(of(0L, V1)));
  }

  @Test
  public void visit_ProofRightValue() throws Exception {
    int listSize = 2;

    ListProof left = new HashNode(H1);

    ListProof right = new ProofListElement(V2);
    when(hashFunction.hashBytes(V2)).thenReturn(H2);

    ListProofBranch root = new ListProofBranch(left, right);

    validator = createListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertTrue(validator.isValid());
    assertThat(validator.getElements(), containsExactly(of(1L, V2)));
  }

  @Test
  public void visit_InvalidBranchHashesAsChildren() throws Exception {
    int listSize = 2;

    ListProof left = new HashNode(H1);
    ListProof right = new HashNode(H2);
    ListProofBranch root = new ListProofBranch(left, right);

    validator = createListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertFalse(validator.isValid());

    expectedException.expect(IllegalStateException.class);
    validator.getElements();
  }

  @Test
  public void visit_InvalidBranchLeftHashNoRight() throws Exception {
    int listSize = 2;

    ListProof left = new HashNode(H1);
    ListProofBranch root = new ListProofBranch(left, null);

    validator = createListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertFalse(validator.isValid());

    expectedException.expectMessage("the tree does not contain any value nodes");
    expectedException.expect(IllegalStateException.class);
    validator.getElements();
  }

  @Test
  public void visit_UnbalancedInTheRightSubTree() throws Exception {
    ListProofBranch root = new ListProofBranch(
        new ListProofBranch(new ProofListElement(V1),
            new HashNode(H2)),
        new ProofListElement(V3) // <-- A value at the wrong depth.
    );

    int listSize = 3;
    validator = createListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertFalse(validator.isValid());

    expectedException.expectMessage("a value node appears at the wrong level");
    expectedException.expect(IllegalStateException.class);
    validator.getElements();
  }

  @Test
  public void visit_UnbalancedInTheLeftSubTree() throws Exception {
    ListProofBranch root = new ListProofBranch(
        new ProofListElement(V1), // <-- A value at the wrong depth.
        new ListProofBranch(new ProofListElement(V2),
            new HashNode(H3))
    );

    int listSize = 3;
    validator = createListProofValidator(ROOT_HASH, listSize);
    validator.visit(root);

    assertFalse(validator.isValid());

    expectedException.expectMessage("a value node appears at the wrong level");
    expectedException.expect(IllegalStateException.class);
    validator.getElements();
  }

  @Test
  public void visit_UnbalancedLeafNodeTooDeep() throws Exception {
    int depth = 4;
    ListProof root = generateLeftLeaningProofTree(depth);

    // A list of size 4 has a height equal to 2, however, the proof tree exceeds that height.
    long listSize = 4;
    validator = createListProofValidator(ROOT_HASH, listSize);

    root.accept(validator);

    assertFalse(validator.isValid());

    expectedException.expectMessage("a value node appears at the wrong level");
    expectedException.expect(IllegalStateException.class);
    validator.getElements();
  }

  private ListProofValidator createListProofValidator(HashCode rootHash, long numElements) {
    return new ListProofValidator(rootHash, numElements, hashFunction);
  }

  private static Matcher<Map<Long, byte[]>> containsExactly(Map<Long, byte[]> expectedElements) {
    checkArgument(expectedElements.keySet().stream()
            .allMatch(index -> index >= 0L),
        "indices must not be negative: %s", expectedElements.keySet());

    return new TypeSafeMatcher<Map<Long, byte[]>>() {
      @Override
      protected boolean matchesSafely(Map<Long, byte[]> actual) {
        if (actual.size() != expectedElements.size()) {
          return false;
        }
        for (Map.Entry<Long, byte[]> e : actual.entrySet()) {
          Long index = e.getKey();
          if (!expectedElements.containsKey(index)) {
            return false;
          }
          byte[] actualValue = e.getValue();
          byte[] expectedValue = expectedElements.get(index);
          if (!Arrays.equals(actualValue, expectedValue)) {
            return false;
          }
        }
        return true;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("elements containing items: ")
            .appendText(byteMapToString(expectedElements));
      }

      private String byteMapToString(Map<Long, byte[]> elements) {
        Map<Long, String> formatted = new TreeMap<>();
        for (Map.Entry<Long, byte[]> entry : elements.entrySet()) {
          formatted.put(entry.getKey(), Arrays.toString(entry.getValue()));
        }
        return formatted.toString();
      }
    };
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
