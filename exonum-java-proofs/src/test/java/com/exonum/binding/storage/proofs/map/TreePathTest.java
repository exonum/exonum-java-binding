/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.storage.proofs.map;

import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.util.BitSet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TreePathTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void emptyPath() {
    TreePath path = new TreePath();
    assertThat(path, equalTo(new TreePath(new BitSet(), 0, Integer.MAX_VALUE)));
    assertThat(path.getLength(), equalTo(0));
  }

  @Test
  public void ctorFailsIfNegativeLength1() {
    expectedException.expect(IllegalArgumentException.class);
    new TreePath(new BitSet(), -1, 1);
  }

  @Test
  public void ctorFailsIfNegativeLengthAndMaxLength() {
    expectedException.expect(IllegalArgumentException.class);
    new TreePath(new BitSet(), -1, -2);
  }

  @Test
  public void ctorFailsIfInvalidLength() {
    expectedException.expect(IllegalArgumentException.class);
    new TreePath(new BitSet(), 2, 1);
  }

  @Test
  public void ctorFailsIfInvalidLengthOfBitSet() {
    expectedException.expect(IllegalArgumentException.class);
    new TreePath(BitSet.valueOf(bytes(0x02)), 1);
  }

  @Test
  public void goLeft() {
    TreePath path = new TreePath();
    path.goLeft();

    assertThat(path, equalTo(new TreePath(BitSet.valueOf(bytes(0x0)), 1)));
    assertThat(path.getLength(), equalTo(1));
  }

  @Test
  public void goRight() {
    TreePath path = new TreePath();
    path.goRight();

    assertThat(path, equalTo(TreePath.valueOf(bytes(0x01))));
    assertThat(path.getLength(), equalTo(1));
  }

  @Test
  public void goLeftThrowsIfMaxLength0IsExceeded() {
    TreePath path = new TreePath(0);

    expectedException.expect(IllegalStateException.class);
    path.goLeft();
  }

  @Test
  public void goLeftThrowsIfMaxLength1IsExceeded() {
    TreePath path = new TreePath(1);
    path.goLeft();

    expectedException.expect(IllegalStateException.class);
    path.goLeft();
  }

  @Test
  public void goRightThrowsIfMaxLength0IsExceeded() {
    TreePath path = new TreePath(0);

    expectedException.expect(IllegalStateException.class);
    path.goRight();
  }

  @Test
  public void goRightThrowsIfMaxLength1IsExceeded() {
    TreePath path = new TreePath(1);
    path.goRight();

    expectedException.expect(IllegalStateException.class);
    path.goRight();
  }
}
