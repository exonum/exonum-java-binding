package com.exonum.binding.storage.proofs.map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.hash.Hashes;
import com.exonum.binding.storage.proofs.map.DbKey.Type;
import java.util.Arrays;
import java.util.BitSet;
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

  private static final int MAX_TREE_HEIGHT = DbKey.KEY_SIZE_BITS;

  private final byte[] expectedRootHash;

  private final byte[] key;

  /**
   * A current path in a proof tree for the Merkle-Patricia BST.
   *
   * <p>Whether the path corresponds to the requested key is checked in terminal nodes only:
   * {@link EqualValueAtRoot}, {@link NonEqualValueAtRoot}, {@link EmptyMapProof},
   *  {@link MappingNotFoundProofBranch}, {@link LeafMapProofNode}.
   */
  private final TreePath bstPath;

  private Status status;

  private byte[] rootHash;

  @Nullable
  private byte[] value;

  /**
   * Creates a new validator of a map proof.
   *
   * @param rootHash a root hash of the proof map to validate against
   * @param key a requested key
   */
  public MapProofValidator(byte[] rootHash, byte[] key) {
    this.expectedRootHash = checkNotNull(rootHash);
    this.key = checkNotNull(key);
    bstPath = new TreePath();
    status = Status.VALID;
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
    bstPath.goLeft();
    node.getLeft().accept(this);

    // TODO: consider returning early if proof is not structurally valid?
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
    bstPath.goRight();
    node.getRight().accept(this);

    // TODO: consider returning early if proof is not structurally valid?
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
    TreePath keyAsPath = getKeyAsBstPath();
    if (!bstPath.isPrefixOf(keyAsPath)) {
      status = Status.INVALID_PATH_TO_NODE;
      return;
    }
    boolean mayContainKeyInSubTrees = Stream.of(node.getLeftKey(), node.getRightKey())
        .map(DbKey::toPath)
        .anyMatch((subTreePath) -> subTreePath.isPrefixOf(keyAsPath));
    if (mayContainKeyInSubTrees) {
      status = Status.MAY_CONTAIN_REQUESTED_VALUE_IN_SUBTREES;
      return;
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
    TreePath keyAsPath = getKeyAsBstPath();
    if (!bstPath.isPrefixOf(keyAsPath)) {
      status = Status.INVALID_PATH_TO_NODE;
      return;
    }
    value = leafMapProofNode.getValue();
    rootHash = Hashes.getHashOf(value);
  }

  private void checkPathToRootNode(MapProof node) {
    checkState(bstPath.getLength() == 0,
        "Invalid state: node=%s cannot appear by such path in any tree: %s", node, bstPath);
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
    return bstPath.getLength() < MAX_TREE_HEIGHT;
  }

  private boolean isValidNotFoundBranchDepth() {
    return bstPath.getLength() < MAX_TREE_HEIGHT - 1;
  }

  private TreePath getKeyAsBstPath() {
    return new TreePath(BitSet.valueOf(key), DbKey.KEY_SIZE_BITS, DbKey.KEY_SIZE_BITS);
  }

  @Override
  public String toString() {
    return "MapProofValidator{"
        + "expectedRootHash=" + Hashes.toString(expectedRootHash)
        + ", rootHash=" + Hashes.toString(rootHash)
        + ", status=" + status
        + ", key=" + Arrays.toString(key)
        + ", value=" + Arrays.toString(value)
        + ", bstPath=" + bstPath
        + '}';
  }
}
