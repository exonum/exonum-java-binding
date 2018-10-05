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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.BitSet;
import java.util.Objects;

/**
 * Represents a path in a binary search tree from the root to a node.
 * Internally path is represented as a BitSet, which is interpreted as follows:
 * if a bit at position {@code i} is <em>set</em>, then at level {@code i} from the root of the BST
 * the path goes <em>right</em>; otherwise — left. For example, [011] ~ [Left, Right, Right].
 *
 * <p>As its primary usage is to track the path as a BST visitor goes down the tree,
 * this class is mutable to avoid {@code O(H^2)} complexity in the most common scenario,
 * where H is the height of the tree.
 */
class TreePath {

  private final BitSet path;

  private final int maxLength;

  /**
   * Actual length of the path.
   *
   * <p>It is <strong>not</strong> the same as {@code path.length()},
   * because the logical length of a path [00…00] is zero.
   */
  private int length;


  /**
   * Creates a new empty unbounded tree path.
   */
  TreePath() {
    this(Integer.MAX_VALUE);
  }

  /**
   * Creates a new empty bounded tree path.
   *
   * @param maxLength the maximum length of the path (= BST height)
   * @throws IllegalArgumentException if the max length is negative
   */
  TreePath(int maxLength) {
    this(new BitSet(), 0, maxLength);
  }

  /**
   * Creates a new unbounded tree path.
   *
   * @param path a path in the BST
   * @param length an actual length of the path
   * @throws NullPointerException if path is null
   * @throws IllegalArgumentException if length is negative;
   *                                  if the specified path has invalid length.
   */
  TreePath(BitSet path, int length) {
    this(path, length, Integer.MAX_VALUE);
  }

  /**
   * Creates a new bounded tree path.
   *
   * @param path a path in the BST
   * @param length an actual length of the path
   * @param maxLength the maximum length of the path (= BST height)
   * @throws NullPointerException if path is null
   * @throws IllegalArgumentException if length is negative or exceeds the max length;
   *                                  if the specified path has invalid length.
   */
  TreePath(BitSet path, int length, int maxLength) {
    checkArgument(0 <= length, "Length must be non-negative: %s", length);
    checkArgument(length <= maxLength,
        "Path length exceeds the maximum length of %s: %s", maxLength, length);
    checkArgument(path.length() <= length,
        "Invalid path: its length=%s exceeds the actual length=%s. Path=%s",
        path.length(), length, path);
    this.path = (BitSet) path.clone();
    this.maxLength = maxLength;
    this.length = length;
  }

  /**
   * A copy constructor.
   *
   * @param other a tree path to copy
   */
  TreePath(TreePath other) {
    this.path = (BitSet) other.path.clone();
    this.maxLength = other.maxLength;
    this.length = other.length;
  }

  void goLeft() {
    assert !path.get(length);
    growLength();
  }

  void goRight() {
    growLength();
    path.set(length - 1);
  }

  private void growLength() {
    checkState(length < maxLength, "Can't grow the length of the path: %s", this);
    length++;
  }

  /**
   * Returns the length of the path (= the number of edges between the root
   * and the current tree node).
   */
  int getLength() {
    return length;
  }

  byte[] toByteArray() {
    return path.toByteArray();
  }

  /**
   * Checks if this path is equal to the specified object. The max length is not compared.
   * @param o an object to compare against
   * @return true if this path is equal to the specified object
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TreePath treePath = (TreePath) o;
    return length == treePath.length
        && Objects.equals(path, treePath.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, length);
  }

  @Override
  public String toString() {
    return "TreePath{"
        + "path=" + path
        + ", length=" + length
        + ", maxLength=" + maxLength
        + '}';
  }

  /**
   * Creates a new tree path from the given path as byte array.
   *
   * @return a new tree path, so that:
   * <ul>
   *   <li>path is equal to {@code BitSet.valueOf(path)}</li>
   *   <li>length is equal to {@code BitSet.valueOf(path).length()}</li>
   *   <li>maxLength is set to {@link Integer#MAX_VALUE}</li>
   * </ul>
   */
  static TreePath valueOf(byte[] path) {
    BitSet bitPath = BitSet.valueOf(path);
    int length = bitPath.length();
    return new TreePath(bitPath, length, Integer.MAX_VALUE);
  }
}
