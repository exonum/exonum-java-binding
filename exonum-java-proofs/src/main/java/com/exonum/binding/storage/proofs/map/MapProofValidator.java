package com.exonum.binding.storage.proofs.map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.hash.Hashes;
import com.exonum.binding.storage.proofs.map.DbKey.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * A validator of map proofs.
 */
public class MapProofValidator implements MapProofVisitor {

  /**
   * Various statuses of the proof: why it may not be valid.
   */
  enum Status {
    VALID("Proof is structurally correct. Have to compare hashes"),
    INVALID_DB_KEY_OF_ROOT_NODE("Root node has unexpected database key"),
    INVALID_BRANCH_NODE_DEPTH("Branch node appears below the maximum allowed depth"),
    INVALID_PATH_TO_NODE("Path to the node must be a prefix of the requested key"),
    MAY_CONTAIN_REQUESTED_VALUE_IN_SUBTREES(
        "Left or right subtree may contain the requested key which contradicts the proof");

    final String description;

    Status() {
      this("");
    }

    Status(String description) {
      this.description = description;
    }
  }

  /**
   * A temporary feature-switch. When enabled:
   *   - DbKey checks that no bits are set after `numSignificant` bits.
   *   - MapProofValidator checks that left and right keys of a node are correct.
   */
  static final boolean PERFORM_TREE_CORRECTNESS_CHECKS = false;

  private static final int MAX_TREE_HEIGHT = DbKey.KEY_SIZE_BITS;

  private final byte[] expectedRootHash;

  private final byte[] key;

  /**
   * A key as a bit set.
   *
   * <p>A visitor of each branch node checks that a child key is a prefix of
   * a requested database key.
   */
  private final KeyBitSet keyBits;

  private Status status;

  private byte[] rootHash;

  @Nullable
  private byte[] value;

  /**
   * A height of the proof tree, calculated as the validator goes down the tree.
   */
  private int bstHeight;

  /**
   * Creates a new validator of a map proof.
   *
   * @param rootHash a root hash of the proof map to validate against
   * @param key a requested key
   */
  public MapProofValidator(byte[] rootHash, byte[] key) {
    this.expectedRootHash = checkNotNull(rootHash);
    this.key = checkNotNull(key);
    keyBits = getKeyAsBitSet(key);
    status = Status.VALID;
    rootHash = new byte[0];
    value = null;
    bstHeight = 0;
  }

  private static KeyBitSet getKeyAsBitSet(byte[] key) {
    return new KeyBitSet(key, DbKey.KEY_SIZE_BITS);
  }

  /**
   * Returns true if this proof is valid: structurally correct and producing the expected root hash.
   */
  public boolean isValid() {
    return status == Status.VALID && Arrays.equals(rootHash, expectedRootHash);
  }

  public byte[] getKey() {
    return key;
  }

  /**
   * Returns the value mapped to the specified key, or {@code Optional.empty()}
   * if there is no mapping for this key.
   *
   * @throws IllegalStateException if the proof is not valid
   */
  public Optional<byte[]> getValue() {
    checkState(isValid(), "Proof is not valid: %s", this);
    return Optional.ofNullable(value);
  }

  @Override
  public void visit(EqualValueAtRoot node) {
    checkPathToRootNode(node);
    if (!hasValidDbKey(node)) {
      status = Status.INVALID_DB_KEY_OF_ROOT_NODE;
      return;
    }
    DbKey databaseKey = node.getDatabaseKey();
    byte[] rawDbKey = databaseKey.getRawDbKey();
    value = node.getValue();
    byte[] valueHash = Hashes.getHashOf(value);
    rootHash = getSingletonTreeHash(rawDbKey, valueHash);
  }

  @Override
  public void visit(NonEqualValueAtRoot node) {
    checkPathToRootNode(node);
    if (!hasValidDbKey(node)) {
      status = Status.INVALID_DB_KEY_OF_ROOT_NODE;
      return;
    }
    DbKey databaseKey = node.getDatabaseKey();
    byte[] rawDbKey = databaseKey.getRawDbKey();
    byte[] valueHash = node.getValueHash().getHash();
    rootHash = getSingletonTreeHash(rawDbKey, valueHash);
  }

  @Override
  public void visit(EmptyMapProof node) {
    checkPathToRootNode(node);
    rootHash = getEmptyTreeHash();
  }

  @Override
  public void visit(LeftMapProofBranch node) {
    if (!isValidBranchDepth()) {
      status = Status.INVALID_BRANCH_NODE_DEPTH;
      return;
    }
    if (PERFORM_TREE_CORRECTNESS_CHECKS) {
      KeyBitSet leftKey = node.getLeftKey().keyBits();
      if (!leftKey.isPrefixOf(keyBits)) {
        status = Status.INVALID_PATH_TO_NODE;
        return;
      }
    }

    bstHeight++;
    node.getLeft().accept(this);

    byte[] leftHash = rootHash;
    byte[] rightHash = node.getRightHash().getHash();
    byte[] leftDbKey = node.getLeftKey().getRawDbKey();
    byte[] rightDbKey = node.getRightKey().getRawDbKey();
    rootHash = Hashes.getHashOf(leftHash, rightHash, leftDbKey, rightDbKey);
  }

  @Override
  public void visit(RightMapProofBranch node) {
    if (!isValidBranchDepth()) {
      status = Status.INVALID_BRANCH_NODE_DEPTH;
      return;
    }
    if (PERFORM_TREE_CORRECTNESS_CHECKS) {
      KeyBitSet rightKey = node.getRightKey().keyBits();
      if (!rightKey.isPrefixOf(keyBits)) {
        status = Status.INVALID_PATH_TO_NODE;
        return;
      }
    }

    bstHeight++;
    node.getRight().accept(this);

    byte[] leftHash = node.getLeftHash().getHash();
    byte[] rightHash = rootHash;
    byte[] leftDbKey = node.getLeftKey().getRawDbKey();
    byte[] rightDbKey = node.getRightKey().getRawDbKey();
    rootHash = Hashes.getHashOf(leftHash, rightHash, leftDbKey, rightDbKey);
  }

  @Override
  public void visit(MappingNotFoundProofBranch node) {
    if (!isValidNotFoundBranchDepth()) {
      status = Status.INVALID_BRANCH_NODE_DEPTH;
      return;
    }
    // This one is absolutely required in a proper validator.
    if (PERFORM_TREE_CORRECTNESS_CHECKS) {
      boolean mayContainKeyInSubTrees = Stream.of(node.getLeftKey(), node.getRightKey())
          .map(DbKey::keyBits)
          .anyMatch((subTreeKey) -> subTreeKey.isPrefixOf(keyBits));
      if (mayContainKeyInSubTrees) {
        status = Status.MAY_CONTAIN_REQUESTED_VALUE_IN_SUBTREES;
        return;
      }
    }

    // The node is correct, compute the hash.
    byte[] leftHash = node.getLeftHash().getHash();
    byte[] rightHash = node.getRightHash().getHash();
    byte[] leftDbKey = node.getLeftKey().getRawDbKey();
    byte[] rightDbKey = node.getRightKey().getRawDbKey();
    rootHash = Hashes.getHashOf(leftHash, rightHash, leftDbKey, rightDbKey);
  }

  @Override
  public void visit(LeafMapProofNode leafMapProofNode) {
    if (bstHeight == 0) {
      status = Status.INVALID_PATH_TO_NODE;
      return;
    }

    value = leafMapProofNode.getValue();
    rootHash = Hashes.getHashOf(value);
  }

  private void checkPathToRootNode(MapProof node) {
    checkState(bstHeight == 0,
        "Invalid state: node=%s cannot appear at such depth in any tree: %s", node, bstHeight);
  }

  private boolean hasValidDbKey(EqualValueAtRoot node) {
    return isDatabaseKeyOfRequestedKey(node.getDatabaseKey());
  }

  private boolean hasValidDbKey(NonEqualValueAtRoot node) {
    DbKey databaseKey = node.getDatabaseKey();
    return databaseKey.getNodeType() == Type.LEAF
        && !Arrays.equals(databaseKey.getKeySlice(), key);
  }

  private boolean isDatabaseKeyOfRequestedKey(DbKey databaseKey) {
    return databaseKey.getNodeType() == Type.LEAF
        && Arrays.equals(databaseKey.getKeySlice(), key);
  }

  private static byte[] getSingletonTreeHash(byte[] dbKey, byte[] valueHash) {
    return Hashes.getHashOf(dbKey, valueHash);
  }

  private static byte[] getEmptyTreeHash() {
    return new byte[Hashes.HASH_SIZE_BYTES];
  }

  private boolean isValidBranchDepth() {
    return bstHeight < MAX_TREE_HEIGHT;
  }

  private boolean isValidNotFoundBranchDepth() {
    return bstHeight < MAX_TREE_HEIGHT - 1;
  }

  @Override
  public String toString() {
    return "MapProofValidator{"
        + "expectedRootHash=" + Hashes.toString(expectedRootHash)
        + ", rootHash=" + Hashes.toString(rootHash)
        + ", status=" + status
        + ", key=" + Arrays.toString(key)
        + ", keyBits=" + keyBits
        + ", value=" + Arrays.toString(value)
        + ", bstHeight=" + bstHeight
        + '}';
  }
}
