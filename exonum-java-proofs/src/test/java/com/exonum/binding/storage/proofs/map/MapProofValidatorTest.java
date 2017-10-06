package com.exonum.binding.storage.proofs.map;

import static com.exonum.binding.storage.proofs.map.MapProofValidatorMatchers.isNotValid;
import static com.exonum.binding.storage.proofs.map.MapProofValidatorMatchers.isValid;
import static com.exonum.binding.test.Bytes.bytes;
import static com.exonum.binding.test.Bytes.createPrefixed;
import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import com.exonum.binding.hash.Hashes;
import com.exonum.binding.storage.proofs.map.DbKey.Type;
import com.exonum.binding.storage.proofs.map.MapProofValidator.Status;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    Hashes.class,
})
public class MapProofValidatorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final byte[] V1 = bytes("V1");
  private static final byte[] ROOT_HASH = createPrefixed(bytes("root hash"),
      Hashes.HASH_SIZE_BYTES);
  private static final byte[] EMPTY_HASH = new byte[Hashes.HASH_SIZE_BYTES];

  private MapProofValidator validator;

  @Before
  public void setUp() throws Exception {
    mockStatic(Hashes.class);
    // Return root hash by default
    when(Hashes.getHashOf(anyBytes())).thenReturn(ROOT_HASH);
  }

  @Test
  public void testVisitEqualAtRoot_OtherKey() throws Exception {
    byte[] key = createKey(0b1011);  // [110100…00]
    byte[] otherKey = createKey(0b101);
    MapProof mapProof = new EqualValueAtRoot(leafDbKey(otherKey), V1);

    validator = new MapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.INVALID_DB_KEY_OF_ROOT_NODE);  // Keys must match.
  }

  @Test
  public void testVisitEqualAtRoot_BranchDbKey() throws Exception {
    byte[] key = createKey(0b1011);  // [110100…00]
    DbKey databaseKey = branchDbKey(key, 4);
    MapProof mapProof = new EqualValueAtRoot(databaseKey, V1);

    validator = new MapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.INVALID_DB_KEY_OF_ROOT_NODE);  // Must be a leaf db key.
  }

  @Test
  public void testVisitEqualAtRoot_FailsIfAlreadyVisitedBranches() throws Exception {
    visitSomeBranches();

    expectedException.expect(IllegalStateException.class);
    validator.visit(new EqualValueAtRoot(leafDbKey(createKey(0x0F)), V1));
  }

  @Test
  public void testVisitEqualAtRoot_Valid() throws Exception {
    byte[] key = createKey(0b1011);  // [110100…00]
    byte[] value = V1;
    MapProof mapProof = new EqualValueAtRoot(leafDbKey(key), value);

    validator = new MapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isValid(key, value));
  }

  @Test
  public void testVisitNonEqualAtRoot_EqualToRequestedKey() throws Exception {
    byte[] key = createKey(0b1011);  // [110100…00]
    MapProof mapProof = new NonEqualValueAtRoot(leafDbKey(key), createHash("h1"));

    validator = new MapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.INVALID_DB_KEY_OF_ROOT_NODE);  // Keys must not match.
  }

  @Test
  public void testVisitNonEqualAtRoot_BranchDbKey() throws Exception {
    byte[] key = createKey(0b101);
    DbKey databaseKey = branchDbKey(key, 4);
    MapProof mapProof = new NonEqualValueAtRoot(databaseKey, createHash("h1"));

    byte[] otherKey = createKey(0b1011);  // [110100…00]
    validator = new MapProofValidator(ROOT_HASH, otherKey);
    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.INVALID_DB_KEY_OF_ROOT_NODE);  // The database key must be a leaf key.
  }

  @Test
  public void testVisitNonEqualAtRoot_FailsIfAlreadyVisitedBranches() throws Exception {
    visitSomeBranches();

    expectedException.expect(IllegalStateException.class);
    validator.visit(new NonEqualValueAtRoot(leafDbKey(createKey(0x0F)),
        createHash("h1")));
  }

  @Test
  public void testVisitNonEqualAtRoot_Valid() throws Exception {
    byte[] key = createKey(0b0100); // [00100…00]
    MapProof mapProof = new NonEqualValueAtRoot(leafDbKey(key),
        createHash("h1"));

    byte[] otherKey = createKey(0b1011);  // [110100…00]
    validator = new MapProofValidator(ROOT_HASH, otherKey);
    mapProof.accept(validator);

    assertThat(validator, isValid(otherKey, null));
  }

  @Test
  public void testVisitEmptyAtRoot_Valid() throws Exception {
    MapProof mapProof = new EmptyMapProof();

    byte[] key = createKey(0b101);
    validator = new MapProofValidator(EMPTY_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isValid(key, null));
  }

  @Test
  public void testVisitEmptyAtRoot_NotValid() throws Exception {
    MapProof mapProof = new EmptyMapProof();

    byte[] key = createKey(0b101);
    validator = new MapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.VALID);  // Hash mismatch.
  }

  @Test
  public void testVisitEmptyAtRoot_FailsIfAlreadyVisitedBranches() throws Exception {
    visitSomeBranches();

    expectedException.expect(IllegalStateException.class);
    validator.visit(new EmptyMapProof());
  }

  /**
   * Just visit some branches to extend the tree path.
   */
  private void visitSomeBranches() {
    byte[] key = createKey(0b100);  // [00100…00]
    MapProof mapProof = new LeftMapProofBranch(
        new MappingNotFoundProofBranch(
            createHash("h1"),
            createHash("h2"),
            branchDbKey(createKey(0b1000), 4),
            branchDbKey(createKey(0b1100), 4)),
        createHash("h3"),
        branchDbKey(createKey(0b00), 2),
        branchDbKey(createKey(0b01), 2));

    validator = new MapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertTrue(validator.isValid());
  }

  @Test
  public void testVisitMappingNotFound_NotValidNotAPrefixOfKey() throws Exception {
    byte[] key = createKey(0b100);  // [00100…00]
    MapProof mapProof = new RightMapProofBranch(
        createHash("h3"),
        // The path: ![1] <- [00100…00]
        new MappingNotFoundProofBranch(
            createHash("h1"),
            createHash("h2"),
            branchDbKey(createKey(0b001), 3),
            branchDbKey(createKey(0b111), 3)),
        branchDbKey(createKey(0), 1),
        branchDbKey(createKey(1), 1));

    validator = new MapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.INVALID_PATH_TO_NODE);
  }

  @Test
  public void testVisitMappingNotFound_NotValidLeftIsPrefixOfKey() throws Exception {
    byte[] key = createKey(0b100);  // [00100…00]
    MapProof mapProof = new LeftMapProofBranch(
        new MappingNotFoundProofBranch(
            createHash("h1"),
            createHash("h2"),
            // The key of the left branch is [00] <- [00100…00]
            branchDbKey(createKey(0b00), 2),
            branchDbKey(createKey(0b10), 2)),
        createHash("h3"),
        branchDbKey(createKey(0b0), 1),
        branchDbKey(createKey(0b1), 1));

    validator = new MapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.MAY_CONTAIN_REQUESTED_VALUE_IN_SUBTREES);
  }

  @Test
  public void testVisitMappingNotFound_NotValidRightIsPrefixOfKey() throws Exception {
    byte[] key = createKey(0b110);  // [01100…00]
    MapProof mapProof = new LeftMapProofBranch(
        new MappingNotFoundProofBranch(
            createHash("h1"),
            createHash("h2"),
            branchDbKey(createKey(0b00), 2),
            // The key of the right branch is [01] <- [01100…00]
            branchDbKey(createKey(0b10), 2)),
        createHash("h3"),
        branchDbKey(createKey(0b0), 1),
        branchDbKey(createKey(0b1), 1));

    validator = new MapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.MAY_CONTAIN_REQUESTED_VALUE_IN_SUBTREES);
  }

  // Successful test
  @Test
  public void testVisitMappingNotFound_Valid() throws Exception {
    byte[] key = createKey(0b100);  // [00100…00]
    MapProof mapProof = new LeftMapProofBranch(
        new MappingNotFoundProofBranch(
            createHash("h1"),
            createHash("h2"),
            branchDbKey(createKey(0b1000), 4),
            branchDbKey(createKey(0b1100), 4)),
        createHash("h3"),
        branchDbKey(createKey(0b00), 2),
        branchDbKey(createKey(0b01), 2));

    validator = new MapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isValid(key, null));
  }

  @Test
  public void testVisitLeaf_NotValid_NotAPrefixOfKey_LeftSubTree() throws Exception {
    byte[] key = createKey(0b101);      // [10100…00]
    byte[] otherKey = createKey(0b100); // [00100…00]
    MapProof mapProof = new LeftMapProofBranch(
        new LeafMapProofNode(V1),  // ![0] <- [10100…00]
        createHash("h1"),
        leafDbKey(otherKey),
        branchDbKey(createKey(0b11), 2));

    validator = new MapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.INVALID_PATH_TO_NODE);
  }

  @Test
  public void testVisitLeaf_NotValid_NotAPrefixOfKey_RightSubTree() throws Exception {
    byte[] key = createKey(0b100);      // [00100…00]
    byte[] otherKey = createKey(0b101); // [10100…00]
    MapProof mapProof = new RightMapProofBranch(
        createHash("h1"),
        new LeafMapProofNode(V1), // ![1] <- [00100…00]
        branchDbKey(createKey(0b0), 1),
        leafDbKey(otherKey));

    validator = new MapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.INVALID_PATH_TO_NODE);
  }

  @Test
  public void testVisitLeaf_Valid_L1_LeftSubTree() throws Exception {
    byte[] key = createKey(0b100);      // [00100…00]
    byte[] value = V1;
    MapProof mapProof = new LeftMapProofBranch(
        new LeafMapProofNode(value), // [0] <- [00100…00]
        createHash("h1"),
        leafDbKey(key),
        branchDbKey(createKey(0b1), 1));

    validator = new MapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isValid(key, value));
  }

  @Test
  public void testVisitLeaf_Valid_L1_RightSubTree() throws Exception {
    byte[] key = createKey(0b101);      // [00100…00]
    byte[] value = V1;
    MapProof mapProof = new RightMapProofBranch(
        createHash("h1"),
        new LeafMapProofNode(value), // [1] <- [10100…00]
        branchDbKey(createKey(0b0), 1),
        leafDbKey(key));

    validator = new MapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isValid(key, value));
  }

  @Test
  public void testVisitLeaf_ValidTree_L1_HashMismatch() throws Exception {
    byte[] key = createKey(0b101);      // [00100…00]
    MapProof mapProof = new RightMapProofBranch(
        createHash("h1"),
        new LeafMapProofNode(V1), // [1] <- [10100…00]
        branchDbKey(createKey(0b0), 1),
        leafDbKey(key));

    when(Hashes.getHashOf(anyBytes())).thenReturn(EMPTY_HASH);
    validator = new MapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.VALID);
  }

  private static byte[][] anyBytes() {
    return any();
  }

  private static byte[] createKey(int... prefix) {
    byte[] prefixBytes = bytes(prefix);
    return createKey(prefixBytes);
  }

  private static byte[] createKey(byte[] prefix) {
    checkArgument(prefix.length <= DbKey.KEY_SIZE);
    return createPrefixed(prefix, DbKey.KEY_SIZE);
  }

  private static HashCode createHash(String prefix) {
    byte[] prefixBytes = bytes(prefix);
    return new HashCode(createPrefixed(prefixBytes, Hashes.HASH_SIZE_BYTES));
  }

  private static DbKey leafDbKey(byte[] key) {
    return new DbKey(Type.LEAF, key, DbKey.KEY_SIZE_BITS);
  }

  private static DbKey branchDbKey(byte[] key, int numSignificantBits) {
    return new DbKey(Type.BRANCH, key, numSignificantBits);
  }

  @Test
  public void testVisitRightLeaningTree_H1_Valid() throws Exception {
    int height = 1;
    byte[] key = new byte[DbKey.KEY_SIZE];
    byte[] value = V1;
    MapProof mapProof = createProofTree(height, value);

    validator = new MapProofValidator(ROOT_HASH, key);

    mapProof.accept(validator);

    assertThat(validator, isValid(key, value));
  }

  @Test
  public void testVisitRightLeaningTree_H256_Valid() throws Exception {
    int height = 256;
    byte[] key = new byte[DbKey.KEY_SIZE];
    byte[] value = V1;
    MapProof mapProof = createProofTree(height, value);

    validator = new MapProofValidator(ROOT_HASH, key);

    mapProof.accept(validator);

    assertThat(validator, isValid(key, value));
  }

  @Test
  public void testVisitRightLeaningTree_H257_NotValid() throws Exception {
    int height = 257;
    byte[] key = new byte[DbKey.KEY_SIZE];
    MapProof mapProof = createProofTree(height, V1);

    validator = new MapProofValidator(ROOT_HASH, key);

    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.INVALID_BRANCH_NODE_DEPTH);
  }

  // TODO: test left-leaning?

  private void testGetValueFails(Status status) {
    String errorMessageRegex = "Proof is not valid:.+"
        + "status=" + Pattern.quote(status.toString()) + ".*";
    expectedException.expectMessage(matchesPattern(errorMessageRegex));
    expectedException.expect(IllegalStateException.class);
    validator.getValue();
  }

  /**
   * Creates a right-leaning map proof tree.
   *
   * <p>The tree below is an example of height 3:
   * <pre>
   *       o
   *      / \
   *     o   h
   *    / \
   *   o   h
   *  / \
   * v   h
   * </pre>
   *
   * @param height a height of the tree. <em>May</em> exceed the maximum allowed height to create
   *               invalid inputs.
   * @param value a value to put into the value node
   * @return a right-leaning map proof tree of the given height. At level equal to the height
   *         it has a value node; all other nodes are LeftMapProofBranch nodes.
   */
  private static MapProof createProofTree(int height, byte[] value) {
    checkArgument(height > 0);

    TreePath path = new TreePath(); // start at the root
    return createProofTreeNode(path, height, value);
  }

  private static MapProofNode createProofTreeNode(TreePath pathToThis, int height, byte[] value) {
    if (height == 0) {
      return new LeafMapProofNode(value);
    }

    TreePath rightPath = new TreePath(pathToThis);
    rightPath.goRight();
    DbKey rightKey = leafDbKey(createKey(stripToKeySize(rightPath.toByteArray())));
    HashCode rightHash = createHash("h1");

    DbKey leftKey = branchDbKey(createKey(stripToKeySize(pathToThis.toByteArray())),
        pathToThis.getLength());
    pathToThis.goLeft();
    return new LeftMapProofBranch(
        createProofTreeNode(pathToThis, height - 1, value),
        rightHash,
        leftKey,
        rightKey
    );
  }

  private static byte[] stripToKeySize(byte[] keyBytes) {
    if (keyBytes.length <= DbKey.KEY_SIZE) {
      return keyBytes;
    }
    return Arrays.copyOf(keyBytes, DbKey.KEY_SIZE);
  }
}
