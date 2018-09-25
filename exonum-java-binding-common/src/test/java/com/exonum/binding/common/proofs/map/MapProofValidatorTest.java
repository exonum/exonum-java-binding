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

package com.exonum.binding.common.proofs.map;

import static com.exonum.binding.common.hash.Hashing.DEFAULT_HASH_SIZE_BYTES;
import static com.exonum.binding.common.proofs.map.DbKeyTestUtils.branchDbKey;
import static com.exonum.binding.common.proofs.map.DbKeyTestUtils.leafDbKey;
import static com.exonum.binding.common.proofs.map.MapProofValidatorMatchers.isNotValid;
import static com.exonum.binding.common.proofs.map.MapProofValidatorMatchers.isValid;
import static com.exonum.binding.test.Bytes.bytes;
import static com.exonum.binding.test.Bytes.createPrefixed;
import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.HashFunction;
import com.exonum.binding.common.hash.Hasher;
import com.exonum.binding.common.proofs.map.MapProofValidator.Status;
import com.exonum.binding.common.serialization.StandardSerializers;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MapProofValidatorTest {


  private static final String V1 = "v1";
  private static final HashCode ROOT_HASH = HashCode.fromBytes(
      createPrefixed(bytes("root hash"), DEFAULT_HASH_SIZE_BYTES));
  private static final HashCode EMPTY_HASH = HashCode.fromBytes(
      new byte[DEFAULT_HASH_SIZE_BYTES]);
  private static final int MAX_SIGNIFICANT_BITS_IN_BRANCH = 255;

  private HashFunction hashFunction;
  private Hasher hasher;
  private MapProofValidator<String> validator;

  @BeforeEach
  void setUp() {
    hasher = mock(Hasher.class);
    when(hasher.putObject(any(), any())).thenReturn(hasher);
    when(hasher.putBytes(any(byte[].class))).thenReturn(hasher);
    when(hasher.hash()).thenReturn(ROOT_HASH);

    hashFunction = mock(HashFunction.class);
    when(hashFunction.hashBytes(any(byte[].class))).thenReturn(ROOT_HASH);
    when(hashFunction.newHasher()).thenReturn(hasher);
  }

  @Test
  void testVisitEqualAtRoot_OtherKey() {
    byte[] key = createKey(0b1011);  // [110100…00]
    byte[] otherKey = createKey(0b101);
    MapProof mapProof = equalValueAtRoot(leafDbKey(otherKey), V1);

    validator = createMapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.INVALID_DB_KEY_OF_ROOT_NODE);  // Keys must match.
  }

  @Test
  void testVisitEqualAtRoot_BranchDbKey() {
    byte[] key = createKey(0b1011);  // [110100…00]
    DbKey databaseKey = branchDbKey(key, 4);
    MapProof mapProof = equalValueAtRoot(databaseKey, V1);

    validator = createMapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.INVALID_DB_KEY_OF_ROOT_NODE);  // Must be a leaf db key.
  }

  @Test
  void testVisitEqualAtRoot_FailsIfAlreadyVisitedBranches() {
    visitSomeBranches();

    assertThrows(IllegalStateException.class,
        () -> validator.visit(equalValueAtRoot(leafDbKey(createKey(0x0F)), V1)));

  }

  @Test
  void testVisitEqualAtRoot_Valid() {
    byte[] key = createKey(0b1011);  // [110100…00]
    String value = V1;
    MapProof mapProof = equalValueAtRoot(leafDbKey(key), value);

    validator = createMapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isValid(key, value));
  }

  @Test
  void testVisitNonEqualAtRoot_EqualToRequestedKey() {
    byte[] key = createKey(0b1011);  // [110100…00]
    MapProof mapProof = new NonEqualValueAtRoot(leafDbKey(key), createHash("h1"));

    validator = createMapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.INVALID_DB_KEY_OF_ROOT_NODE);  // Keys must not match.
  }

  @Test
  void testVisitNonEqualAtRoot_BranchDbKey() {
    byte[] key = createKey(0b101);
    DbKey databaseKey = branchDbKey(key, 4);
    MapProof mapProof = new NonEqualValueAtRoot(databaseKey, createHash("h1"));

    byte[] otherKey = createKey(0b1011);  // [110100…00]
    validator = createMapProofValidator(ROOT_HASH, otherKey);
    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.INVALID_DB_KEY_OF_ROOT_NODE);  // The database key must be a leaf key.
  }

  @Test
  void testVisitNonEqualAtRoot_FailsIfAlreadyVisitedBranches() {
    visitSomeBranches();


    assertThrows(IllegalStateException.class,
        () -> validator.visit(new NonEqualValueAtRoot(leafDbKey(createKey(0x0F)),
            createHash("h1"))));

  }

  @Test
  void testVisitNonEqualAtRoot_Valid() {
    byte[] key = createKey(0b0100); // [00100…00]
    MapProof mapProof = new NonEqualValueAtRoot(leafDbKey(key),
        createHash("h1"));

    byte[] otherKey = createKey(0b1011);  // [110100…00]
    validator = createMapProofValidator(ROOT_HASH, otherKey);
    mapProof.accept(validator);

    assertThat(validator, isValid(otherKey, null));
  }

  @Test
  void testVisitEmptyAtRoot_Valid() {
    MapProof mapProof = new EmptyMapProof();

    byte[] key = createKey(0b101);
    validator = createMapProofValidator(EMPTY_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isValid(key, null));
  }

  @Test
  void testVisitEmptyAtRoot_NotValid() {
    MapProof mapProof = new EmptyMapProof();

    byte[] key = createKey(0b101);
    validator = createMapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.VALID);  // Hash mismatch.
  }

  @Test
  void testVisitEmptyAtRoot_FailsIfAlreadyVisitedBranches() {
    visitSomeBranches();


    assertThrows(IllegalStateException.class, () -> validator.visit(new EmptyMapProof()));

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

    validator = createMapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertTrue(validator.isValid());
  }

  @Test
  void testVisitMappingNotFound_NotValidNotAPrefixOfKey() {
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

    validator = createMapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    if (MapProofValidator.PERFORM_TREE_CORRECTNESS_CHECKS) {
      assertThat(validator, isNotValid());
      testGetValueFails(Status.INVALID_PATH_TO_NODE);
    }
  }

  @Test
  void testVisitMappingNotFound_NotValidLeftIsPrefixOfKey() {
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

    validator = createMapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    if (MapProofValidator.PERFORM_TREE_CORRECTNESS_CHECKS) {
      assertThat(validator, isNotValid());
      testGetValueFails(Status.MAY_CONTAIN_REQUESTED_VALUE_IN_SUBTREES);
    }
  }

  @Test
  void testVisitMappingNotFound_NotValidRightIsPrefixOfKey() {
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

    validator = createMapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    if (MapProofValidator.PERFORM_TREE_CORRECTNESS_CHECKS) {
      assertThat(validator, isNotValid());
      testGetValueFails(Status.MAY_CONTAIN_REQUESTED_VALUE_IN_SUBTREES);
    }
  }

  // Successful test
  @Test
  void testVisitMappingNotFound_ValidAtRoot() {
    byte[] key = createKey(0b0100);  // [00100…00]
    MapProof mapProof = new MappingNotFoundProofBranch(
        createHash("h1"),
        createHash("h2"),
        branchDbKey(createKey(0b0000), 4),
        branchDbKey(createKey(0b1001), 4));

    validator = createMapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isValid(key, null));
  }

  @Test
  void testVisitMappingNotFound_ValidAsLeftChild() {
    byte[] key = createKey(0b0100);  // [00100…00]
    MapProof mapProof = new LeftMapProofBranch(
        new MappingNotFoundProofBranch(
            createHash("h1"),
            createHash("h2"),
            branchDbKey(createKey(0b00_00), 4),
            branchDbKey(createKey(0b11_00), 4)),
        createHash("h3"),
        branchDbKey(createKey(0b00), 2),
        branchDbKey(createKey(0b01), 2));

    validator = createMapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isValid(key, null));
  }

  @Test
  void testVisitLeaf_NotValid_NotAPrefixOfKey_LeftSubTree() {
    byte[] key = createKey(0b101);      // [10100…00]
    byte[] otherKey = createKey(0b100); // [00100…00]
    MapProof mapProof = new LeftMapProofBranch(
        leafNode(V1),  // ![0] <- [10100…00]
        createHash("h1"),
        leafDbKey(otherKey),
        branchDbKey(createKey(0b11), 2));

    validator = createMapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    if (MapProofValidator.PERFORM_TREE_CORRECTNESS_CHECKS) {
      assertThat(validator, isNotValid());
      testGetValueFails(Status.INVALID_PATH_TO_NODE);
    }
  }

  @Test
  void testVisitLeaf_NotValid_NotAPrefixOfKey_RightSubTree() {
    byte[] key = createKey(0b100);      // [00100…00]
    byte[] otherKey = createKey(0b101); // [10100…00]
    MapProof mapProof = new RightMapProofBranch(
        createHash("h1"),
        leafNode(V1), // ![1] <- [00100…00]
        branchDbKey(createKey(0b0), 1),
        leafDbKey(otherKey));

    validator = createMapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    if (MapProofValidator.PERFORM_TREE_CORRECTNESS_CHECKS) {
      assertThat(validator, isNotValid());
      testGetValueFails(Status.INVALID_PATH_TO_NODE);
    }
  }

  @Test
  void testVisitLeaf_NotValid_LeafAtRoot() {
    byte[] key = createKey(0b100);      // [00100…00]
    MapProof mapProof = leafNode(V1);

    validator = new MapProofValidator<>(ROOT_HASH, key, StandardSerializers.string());
    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.INVALID_PATH_TO_NODE);
  }

  @Test
  void testVisitLeaf_Valid_L1_LeftSubTree() {
    byte[] key = createKey(0b100);      // [00100…00]
    String value = V1;
    MapProof mapProof = new LeftMapProofBranch(
        leafNode(value), // [0] <- [00100…00]
        createHash("h1"),
        leafDbKey(key),
        branchDbKey(createKey(0b1), 1));

    validator = createMapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isValid(key, value));
  }

  /**
   * Tests a case when a key length increases for more than a single bit.
   * <pre>
   *                        o
   * +0                 /        \
   *                 o[0110]    o[1…]
   * +0           /         \
   *      L[0110’0111’0…0]  o[0110’1…]
   * </pre>
   */
  @Test
  void testVisitLeaf_Valid_L2_LeftSubTree_PrefixExtension() {
    byte[] key = createKey(0b1110_1110);      // [011101110…00]
    String value = V1;
    MapProof mapProof = new LeftMapProofBranch(
        new LeftMapProofBranch(
            leafNode(value),
            createHash("h2"),
            leafDbKey(key),
            branchDbKey(createKey(0b1_1110), 5)
        ),
        createHash("h1"),
        // Prefix extension: left branch adds 1+3 bits to the prefix.
        branchDbKey(createKey(0b1110), 4),
        branchDbKey(createKey(0b1), 1));

    validator = createMapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isValid(key, value));
  }

  @Test
  void testVisitLeaf_Valid_L1_RightSubTree() {
    byte[] key = createKey(0b101);      // [00100…00]
    String value = V1;
    MapProof mapProof = new RightMapProofBranch(
        createHash("h1"),
        leafNode(value), // [1] <- [10100…00]
        branchDbKey(createKey(0b0), 1),
        leafDbKey(key));

    validator = createMapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isValid(key, value));
  }

  @Test
  void testVisitLeaf_Valid_L2_RightSubTree_PrefixExtension() {
    byte[] key = createKey(0b1_1001);      // [1000100…0]
    String value = V1;
    MapProof mapProof = new RightMapProofBranch(
        createHash("h1"),
        new RightMapProofBranch(
            createHash("h2"),
            leafNode(value),
            branchDbKey(createKey(0b0_1001), 5),
            leafDbKey(key)
        ),
        branchDbKey(createKey(0b0), 1),
        // Prefix extension: right branch adds 1+3 bits to the prefix.
        branchDbKey(createKey(0b1001), 4));

    validator = createMapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isValid(key, value));
  }

  @Test
  void testVisitLeaf_ValidTree_L1_HashMismatch() {
    byte[] key = createKey(0b101);      // [10100…00]
    MapProof mapProof = new RightMapProofBranch(
        createHash("h1"),
        leafNode(V1), // [1] <- [10100…00]
        branchDbKey(createKey(0b0), 1),
        leafDbKey(key));

    when(hasher.hash()).thenReturn(EMPTY_HASH);

    validator = createMapProofValidator(ROOT_HASH, key);
    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.VALID);
  }

  /**
   * Creates a {@link MapProofValidator} with the given rootHash and key and
   * the mock of a hash function.
   */
  private MapProofValidator<String> createMapProofValidator(HashCode rootHash, byte[] key) {
    return new MapProofValidator<>(rootHash, key, StandardSerializers.string(), hashFunction);
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
    return HashCode.fromBytes(createPrefixed(prefixBytes, DEFAULT_HASH_SIZE_BYTES));
  }

  @Test
  void testVisitRightLeaningTree_H1_Valid() {
    int height = 1;
    byte[] key = new byte[DbKey.KEY_SIZE];
    String value = V1;
    MapProof mapProof = createProofTree(height, value);

    validator = createMapProofValidator(ROOT_HASH, key);

    mapProof.accept(validator);

    assertThat(validator, isValid(key, value));
  }

  @Test
  void testVisitRightLeaningTree_H256_Valid() {
    int height = 256;
    byte[] key = new byte[DbKey.KEY_SIZE];
    String value = V1;
    MapProof mapProof = createProofTree(height, value);

    validator = createMapProofValidator(ROOT_HASH, key);

    mapProof.accept(validator);

    assertThat(validator, isValid(key, value));
  }

  @Test
  void testVisitRightLeaningTree_H257_NotValid() {
    int height = 257;
    byte[] key = new byte[DbKey.KEY_SIZE];
    MapProof mapProof = createProofTree(height, V1);

    validator = createMapProofValidator(ROOT_HASH, key);

    mapProof.accept(validator);

    assertThat(validator, isNotValid());
    testGetValueFails(Status.INVALID_BRANCH_NODE_DEPTH);
  }

  // TODO: test left-leaning?

  private void testGetValueFails(Status status) {
    String errorMessageRegex = "Proof is not valid:.+"
        + "status=" + Pattern.quote(status.toString()) + ".*";

    IllegalStateException thrown = assertThrows(IllegalStateException.class,
        () -> validator.getValue());
    assertThat(thrown.getMessage(), matchesPattern(errorMessageRegex));
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
  private static MapProof createProofTree(int height, String value) {
    checkArgument(height > 0);

    TreePath path = new TreePath(); // start at the root
    return createProofTreeNode(path, height, value);
  }

  private static MapProofNode createProofTreeNode(TreePath pathToThis, int height, String value) {
    if (height == 0) {
      return leafNode(value);
    }

    TreePath rightPath = new TreePath(pathToThis);
    rightPath.goRight();
    DbKey rightKey = leafDbKey(createKey(stripToKeySize(rightPath.toByteArray())));
    HashCode rightHash = createHash("h1");

    // MAX_SIGNIFICANT_BITS_IN_BRANCH is used in case when proof tree with height of more than 256
    // is created and we want to keep number of significant bits in branch node less than that
    DbKey leftKey = branchDbKey(createKey(stripToKeySize(pathToThis.toByteArray())),
        Math.min(pathToThis.getLength(), MAX_SIGNIFICANT_BITS_IN_BRANCH));
    pathToThis.goLeft();
    return new LeftMapProofBranch(
        createProofTreeNode(pathToThis, height - 1, value),
        rightHash,
        leftKey,
        rightKey
    );
  }

  private static LeafMapProofNode leafNode(String value) {
    return new LeafMapProofNode(bytesOf(value));
  }

  private static EqualValueAtRoot equalValueAtRoot(DbKey key, String value) {
    return new EqualValueAtRoot(key, bytesOf(value));
  }

  private static byte[] bytesOf(String value) {
    return StandardSerializers.string().toBytes(value);
  }

  private static byte[] stripToKeySize(byte[] keyBytes) {
    if (keyBytes.length <= DbKey.KEY_SIZE) {
      return keyBytes;
    }
    return Arrays.copyOf(keyBytes, DbKey.KEY_SIZE);
  }
}
