package com.exonum.binding.storage.proofs.map;

import static com.exonum.binding.hash.Funnels.hashCodeFunnel;
import static com.exonum.binding.storage.proofs.DbKeyFunnel.dbKeyFunnel;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.HashFunction;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.storage.proofs.map.DbKey.Type;
import com.exonum.binding.storage.serialization.CheckingSerializerDecorator;
import com.exonum.binding.storage.serialization.Serializer;
import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * A validator of map proofs.
 *
 * @param <V> the type of values in the corresponding map
 */
public final class MapProofValidator<V> implements MapProofVisitor {

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

    Status(String description) {
      this.description = description;
    }
  }

  /**
   * A temporary feature-switch. When enabled:
   *   - DbKey checks that no bits are set after `numSignificant` bits.
   *   - MapProofValidator checks that left and right keys of a node are correct.
   */
  static final boolean PERFORM_TREE_CORRECTNESS_CHECKS = true;

  private static final int HASH_SIZE_BITS = Hashing.DEFAULT_HASH_SIZE_BYTES * Byte.SIZE;

  private static final int MAX_TREE_HEIGHT = DbKey.KEY_SIZE_BITS;

  private final HashCode expectedRootHash;

  private final byte[] key;

  private final CheckingSerializerDecorator<V> valueSerializer;

  private final HashFunction hashFunction;

  /**
   * A key as a bit set.
   *
   * <p>A visitor of each branch node checks that a child key is a prefix of
   * a requested database key.
   */
  private final KeyBitSet keyBits;

  private Status status;

  private HashCode rootHash;

  @Nullable
  private V value;

  /**
   * A height of the proof tree, calculated as the validator goes down the tree.
   */
  private int bstHeight;

  /**
   * Creates a new validator of a map proof.
   *
   * @param rootHash a root hash of the proof map to validate against
   * @param key a requested key
   * @param valueSerializer a serializer of map values
   */
  public MapProofValidator(byte[] rootHash, byte[] key, Serializer<V> valueSerializer) {
    this(HashCode.fromBytes(rootHash), key, valueSerializer, Hashing.defaultHashFunction());
  }

  /**
   * Creates a new validator of a map proof.
   *
   * @param rootHash a root hash of the proof map to validate against
   * @param key a requested key
   * @param valueSerializer a serializer of map values
   */
  public MapProofValidator(HashCode rootHash, byte[] key, Serializer<V> valueSerializer) {
    this(rootHash, key, valueSerializer, Hashing.defaultHashFunction());
  }

  @VisibleForTesting  // to easily inject a mock of a hash function.
  MapProofValidator(HashCode rootHash, byte[] key, Serializer<V> valueSerializer,
                    HashFunction hashFunction) {
    checkArgument(rootHash.bits() == HASH_SIZE_BITS,
        "Root hash must be 256 bit long (actual length %s)", rootHash.bits());
    this.expectedRootHash = rootHash;
    this.key = checkNotNull(key);
    this.valueSerializer = CheckingSerializerDecorator.from(valueSerializer);
    keyBits = getKeyAsBitSet(key);
    this.hashFunction = checkNotNull(hashFunction);
    status = Status.VALID;
    rootHash = null;
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
    return status == Status.VALID && expectedRootHash.equals(rootHash);
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
  public Optional<V> getValue() {
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
    byte[] dbValue = node.getValue();
    setValue(dbValue);
    HashCode valueHash = hashFunction.hashBytes(dbValue);
    rootHash = getSingletonTreeHash(node.getDatabaseKey(), valueHash);
  }

  @Override
  public void visit(NonEqualValueAtRoot node) {
    checkPathToRootNode(node);
    if (!hasValidDbKey(node)) {
      status = Status.INVALID_DB_KEY_OF_ROOT_NODE;
      return;
    }
    rootHash = getSingletonTreeHash(node.getDatabaseKey(), node.getValueHash());
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

    HashCode leftHash = rootHash;
    rootHash = computeBranchHash(leftHash,
        node.getRightHash(),
        node.getLeftKey(),
        node.getRightKey());
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

    HashCode rightHash = rootHash;
    rootHash = computeBranchHash(node.getLeftHash(),
        rightHash,
        node.getLeftKey(),
        node.getRightKey());
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
    rootHash = computeBranchHash(node.getLeftHash(),
        node.getRightHash(),
        node.getLeftKey(),
        node.getRightKey());
  }

  @Override
  public void visit(LeafMapProofNode leafMapProofNode) {
    if (bstHeight == 0) {
      status = Status.INVALID_PATH_TO_NODE;
      return;
    }

    byte[] dbValue = leafMapProofNode.getValue();
    setValue(dbValue);
    rootHash = hashFunction.hashBytes(dbValue);
  }

  private void setValue(byte[] serializedValue) {
    checkState(value == null, "Setting the value for the 2nd time, current value=%s", value);
    value = valueSerializer.fromBytes(serializedValue);
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

  private HashCode getSingletonTreeHash(DbKey dbKey, HashCode valueHash) {
    return hashFunction.newHasher()
        .putObject(dbKey, dbKeyFunnel())
        .putObject(valueHash, hashCodeFunnel())
        .hash();
  }

  private static HashCode getEmptyTreeHash() {
    return HashCode.fromBytes(new byte[Hashing.DEFAULT_HASH_SIZE_BYTES]);
  }

  private HashCode computeBranchHash(HashCode leftHash, HashCode rightHash,
                                     DbKey leftDbKey, DbKey rightDbKey) {
    return hashFunction.newHasher()
        .putObject(leftHash, hashCodeFunnel())
        .putObject(rightHash, hashCodeFunnel())
        .putObject(leftDbKey, dbKeyFunnel())
        .putObject(rightDbKey, dbKeyFunnel())
        .hash();
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
        + "expectedRootHash=" + expectedRootHash
        + ", rootHash=" + rootHash
        + ", status=" + status
        + ", key=" + Arrays.toString(key)
        + ", keyBits=" + keyBits
        + ", value=" + value
        + ", bstHeight=" + bstHeight
        + '}';
  }
}
