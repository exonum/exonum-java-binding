package com.exonum.binding.storage.proofs.list;

import static com.exonum.binding.hash.Funnels.hashCodeFunnel;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.HashFunction;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.hash.PrimitiveSink;
import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * A validator of list proofs.
 */
public class ListProofValidator implements ListProofVisitor {

  private final HashCode expectedRootHash;

  private final HashFunction hashFunction;

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

  private HashCode hash;

  /**
    * Creates a new ListProofValidator.
   *
   * @param expectedRootHash an expected value of a root hash
   * @param numElements the number of elements in the proof list.
   *                    The same as the number of leaf nodes in the Merkle tree.
   */
  public ListProofValidator(byte[] expectedRootHash, long numElements) {
    this(HashCode.fromBytes(expectedRootHash), numElements);
  }

  /**
   * Creates a new ListProofValidator.
   *
   * @param expectedRootHash an expected value of a root hash
   * @param numElements the number of elements in the proof list.
   *                    The same as the number of leaf nodes in the Merkle tree.
   */
  public ListProofValidator(HashCode expectedRootHash, long numElements) {
    this(expectedRootHash, numElements, Hashing.defaultHashFunction());
  }

  /**
   * Creates a new ListProofValidator.
   *
   * @param expectedRootHash an expected value of a root hash
   * @param numElements the number of elements in the proof list.
   *                    The same as the number of leaf nodes in the Merkle tree.
   * @param hashFunction a hashFunction to use
   */
  @VisibleForTesting
  ListProofValidator(HashCode expectedRootHash, long numElements, HashFunction hashFunction) {
    checkArgument(0 < numElements, "numElements (%s) must be positive", numElements);
    this.expectedRootHash = checkNotNull(expectedRootHash);
    this.hashFunction = checkNotNull(hashFunction);
    expectedLeafDepth = getExpectedLeafDepth(numElements);
    elements = new TreeMap<>();
    index = 0;
    depth = 0;
    isBalanced = true;
    hash = null;
  }

  private int getExpectedLeafDepth(long numElements) {
    return (int) Math.round(Math.ceil(Math.log(numElements) / Math.log(2.0)));
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
    HashCode leftHash = visitLeft(branch, branchIndex, branchDepth);
    Optional<HashCode> rightHash = visitRight(branch, branchIndex, branchDepth);

    hash = hashFunction.newHasher()
        .putObject(leftHash, hashCodeFunnel())
        .putObject(rightHash, (Optional<HashCode> from, PrimitiveSink into) ->
            from.ifPresent((hash) -> hashCodeFunnel().funnel(hash, into)))
        .hash();
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
      hash = hashFunction.hashObject(value, ProofListElement.funnel());
    }
  }

  /**
   * Returns true if the branch node exceeds the maximum depth at which branch nodes may appear
   * (expectedLeafDepth - 1).
   */
  private boolean exceedsMaxBranchDepth() {
    return depth >= expectedLeafDepth;
  }

  private HashCode visitLeft(ListProofBranch branch, long branchIndex, int branchDepth) {
    index = 2 * branchIndex;
    depth = getChildDepth(branchDepth);
    branch.getLeft().accept(this);
    return hash;
  }

  private Optional<HashCode> visitRight(ListProofBranch branch, long branchIndex, int branchDepth) {
    index = 2 * branchIndex + 1;
    depth = getChildDepth(branchDepth);
    hash = null;
    branch.getRight().ifPresent((right) -> right.accept(this));
    return Optional.ofNullable(hash);
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
    return expectedRootHash.equals(hash);
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
      return "hash mismatch: expected=" + expectedRootHash
          + ", actual=" + hash;
    } else {
      return "";
    }
  }

  @Override
  public String toString() {
    return "ListProofValidator{"
        + "hash=" + hash
        + ", expectedRootHash=" + expectedRootHash
        + ", isBalanced=" + isBalanced
        + ", elements=" + elements
        + ", expectedLeafDepth=" + expectedLeafDepth
        + ", depth=" + depth
        + ", index=" + index
        + '}';
  }
}
