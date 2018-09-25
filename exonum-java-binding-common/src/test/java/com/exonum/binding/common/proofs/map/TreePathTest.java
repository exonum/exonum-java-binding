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

import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.BitSet;
import org.junit.jupiter.api.Test;

class TreePathTest {

  @Test
  void emptyPath() {
    TreePath path = new TreePath();
    assertThat(path, equalTo(new TreePath(new BitSet(), 0, Integer.MAX_VALUE)));
    assertThat(path.getLength(), equalTo(0));
  }

  @Test
  void ctorFailsIfNegativeLength1() {
    assertThrows(IllegalArgumentException.class, () -> new TreePath(new BitSet(), -1, 1));
  }

  @Test
  void ctorFailsIfNegativeLengthAndMaxLength() {
    assertThrows(IllegalArgumentException.class, () -> new TreePath(new BitSet(), -1, -2));
  }

  @Test
  void ctorFailsIfInvalidLength() {
    assertThrows(IllegalArgumentException.class, () -> new TreePath(new BitSet(), 2, 1));
  }

  @Test
  void ctorFailsIfInvalidLengthOfBitSet() {
    assertThrows(IllegalArgumentException.class,
        () -> new TreePath(BitSet.valueOf(bytes(0x02)), 1));
  }

  @Test
  void goLeft() {
    TreePath path = new TreePath();
    path.goLeft();

    assertThat(path, equalTo(new TreePath(BitSet.valueOf(bytes(0x0)), 1)));
    assertThat(path.getLength(), equalTo(1));
  }

  @Test
  void goRight() {
    TreePath path = new TreePath();
    path.goRight();

    assertThat(path, equalTo(TreePath.valueOf(bytes(0x01))));
    assertThat(path.getLength(), equalTo(1));
  }

  @Test
  void goLeftThrowsIfMaxLength0IsExceeded() {
    TreePath path = new TreePath(0);

    assertThrows(IllegalStateException.class, () -> path.goLeft());
  }

  @Test
  void goLeftThrowsIfMaxLength1IsExceeded() {
    TreePath path = new TreePath(1);
    path.goLeft();

    assertThrows(IllegalStateException.class, () -> path.goLeft());
  }

  @Test
  void goRightThrowsIfMaxLength0IsExceeded() {
    TreePath path = new TreePath(0);

    assertThrows(IllegalStateException.class, () -> path.goRight());
  }

  @Test
  void goRightThrowsIfMaxLength1IsExceeded() {
    TreePath path = new TreePath(1);
    path.goRight();

    assertThrows(IllegalStateException.class, () -> path.goRight());
  }
}
