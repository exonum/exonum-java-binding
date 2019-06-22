/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.core.storage.indices;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.ModificationCounter;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.database.View;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class AbstractIndexProxyTest {

  private static final String INDEX_NAME = "index_name";


  private AbstractIndexProxy proxy;

  @Test
  void testConstructor() {
    View view = createFork();
    proxy = new IndexProxyImpl(view);

    assertThat(proxy.dbView, equalTo(view));
  }

  @Test
  void constructorFailsIfNullView() {
    View dbView = null;

    assertThrows(NullPointerException.class, () -> proxy = new IndexProxyImpl(dbView));
  }

  @Test
  void notifyModifiedThrowsIfSnapshotPassed() {
    Snapshot dbView = createSnapshot();
    ModificationCounter counter = dbView.getModificationCounter();
    int initialValue = counter.getCurrentValue();

    proxy = new IndexProxyImpl(dbView);

    UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
        () -> proxy.notifyModified());

    Pattern pattern = Pattern.compile("Cannot modify the view: .*[Ss]napshot.*"
        + "\\nUse a Fork to modify any collection\\.", Pattern.MULTILINE);
    assertThat(thrown.getMessage(), matchesPattern(pattern));

    assertFalse(counter.isModifiedSince(initialValue));
  }

  @Test
  void notifyModifiedAcceptsFork() {
    Fork dbView = createFork();
    ModificationCounter counter = dbView.getModificationCounter();
    int initialValue = counter.getCurrentValue();

    proxy = new IndexProxyImpl(dbView);
    proxy.notifyModified();

    assertTrue(counter.isModifiedSince(initialValue));
  }

  @Test
  void name() {
    Snapshot dbView = createSnapshot();
    proxy = new IndexProxyImpl(dbView);

    assertThat(proxy.getName(), equalTo(INDEX_NAME));
  }

  /**
   * Create a non-owning fork.
   */
  private Fork createFork() {
    return Fork.newInstance(0x01, false, new Cleaner());
  }

  /**
   * Create a non-owning snapshot.
   */
  private Snapshot createSnapshot() {
    return Snapshot.newInstance(0x02, false, new Cleaner());
  }

  private static class IndexProxyImpl extends AbstractIndexProxy {

    private static final long NATIVE_HANDLE = 0x11L;

    IndexProxyImpl(View view) {
      super(new NativeHandle(NATIVE_HANDLE), INDEX_NAME, view);
    }
  }

}
