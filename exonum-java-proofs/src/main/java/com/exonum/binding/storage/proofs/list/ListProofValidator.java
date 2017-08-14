package com.exonum.binding.storage.proofs.list;

import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.hash.Hashes;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 * A validator of list proofs.
 */
public class ListProofValidator implements ListProofVisitor {

  private static final byte[] EMPTY_HASH = new byte[0];

  private final byte[] expectedRootHash;

  private final int expectedLeafDepth;

  private final Map<Long, byte[]> elements;

  private long index;

  private int depth;

  /**
   * Whether a proof tree is «balanced», i.e., contains leaf elements at the bottom level
   * of the tree only.
   *
   * <p>If the proof tree is <strong>not</strong> balanced, it is not valid.
   */
  private boolean isBalanced;

  private byte[] hash;

  /**
   * Creates a new ListProofValidator.
   *
   * @param expectedRootHash an expected value of a root hash
   * @param numElements the number of elements in the proof list.
   *                    The same as the number of leaf nodes in the Merkle tree.
   */
  public ListProofValidator(byte[] expectedRootHash, long numElements) {
    this.expectedRootHash = expectedRootHash.clone();
    expectedLeafDepth = getExpectedLeafDepth(numElements);
    elements = new TreeMap<>();
    index = 0;
    depth = 0;
    isBalanced = true;
    hash = EMPTY_HASH;
  }

  private int getExpectedLeafDepth(long numElements) {
    return (numElements > 1)
        ? (int) Math.round(Math.ceil(Math.log(numElements) / Math.log(2.0)))
        : 1;
  }

  @Override
  public void visit(ListProofBranch branch) {
    if (exceedsMaxBranchDepth()) {
      // A proof tree where a branch node appears below the maximum allowed depth is not valid.
      isBalanced = false;
      return;
    }
    long branchIndex = index;
    int branchDepth = depth;
    byte[] leftHash = visitLeft(branch, branchIndex, branchDepth);
    byte[] rightHash = visitRight(branch, branchIndex, branchDepth);
    hash = Hashes.getHashOf(leftHash, rightHash);
  }

  @Override
  public void visit(HashNode hashNode) {
    this.hash = hashNode.getHash();
  }

  @Override
  public void visit(ProofListElement value) {
    assert !elements.containsKey(index) :
        "Error: already an element by such index in the map: i=" + index
            + ", e=" + Arrays.toString(elements.get(index));

    isBalanced &= (depth == expectedLeafDepth);
    if (isBalanced) {
      elements.put(index, value.getElement());
      hash = Hashes.getHashOf(value.getElement());
    }
  }

  /**
   * Returns true if the branch node exceeds the maximum depth at which branch nodes may appear
   * (expectedLeafDepth - 1).
   */
  private boolean exceedsMaxBranchDepth() {
    return depth >= expectedLeafDepth;
  }

  private byte[] visitLeft(ListProofBranch branch, long branchIndex, int branchDepth) {
    index = branchIndex << 1;
    depth = getChildDepth(branchDepth);
    branch.getLeft().accept(this);
    return hash;
  }

  private byte[] visitRight(ListProofBranch branch, long branchIndex, int branchDepth) {
    index = (branchIndex << 1) + 1;
    depth = getChildDepth(branchDepth);
    hash = EMPTY_HASH;
    branch.getRight().ifPresent((right) -> right.accept(this));
    return hash;
  }

  private int getChildDepth(int branchDepth) {
    return branchDepth + 1;
  }

  /**
   * Returns true if this proof is valid.
   */
  public boolean isValid() {
    return isBalanced && !elements.isEmpty() && doesHashMatchExpected();
  }

  private boolean doesHashMatchExpected() {
    return Arrays.equals(hash, expectedRootHash);
  }

  /**
   * Returns a non-empty collection of list entries: index-element pairs, ordered by indices.
   *
   * @throws IllegalStateException if proof is not valid
   */
  public Map<Long, byte[]> getElements() {
    checkState(isValid(), "Proof is not valid: %s", getReason());
    return elements;
  }

  private String getReason() {
    if (!isBalanced) {
      return "a value node appears at the wrong level";
    } else if (elements.isEmpty()) {
      return "the tree does not contain any value nodes";
    } else if (!doesHashMatchExpected()) {
      return "hash mismatch: expected=" + Hashes.toString(expectedRootHash)
          + ", actual=" + Hashes.toString(hash);
    } else {
      return "";
    }
  }

  @Override
  public String toString() {
    return "ListProofValidator{"
        + "hash=" + Hashes.toString(hash)
        + ", expectedRootHash=" + Hashes.toString(expectedRootHash)
        + ", isBalanced=" + isBalanced
        + ", elements=" + elements
        + ", expectedLeafDepth=" + expectedLeafDepth
        + ", depth=" + depth
        + ", index=" + index
        + '}';
  }
}
