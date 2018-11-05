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

package com.exonum.binding.storage.indices;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.database.ViewModificationCounter;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest(ViewModificationCounter.class)
//TODO disabled since jUnit5 doesn't support PowerMock yet.
@Disabled
class AbstractIndexProxyTest {

  private static final String INDEX_NAME = "index_name";

  @Mock
  private ViewModificationCounter modCounter;

  private AbstractIndexProxy proxy;

  @BeforeEach
  void setUp() {
    mockStatic(ViewModificationCounter.class);
    when(ViewModificationCounter.getInstance()).thenReturn(modCounter);
  }

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
    proxy = new IndexProxyImpl(dbView);

    Pattern pattern = Pattern.compile("Cannot modify the view: .*[Ss]napshot.*"
        + "\\nUse a Fork to modify any collection\\.", Pattern.MULTILINE);

    UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
        () -> proxy.notifyModified());
    assertThat(thrown.getMessage(), matchesPattern(pattern));
  }

  @Test
  void notifyModifiedAcceptsFork() {
    Fork dbView = createFork();
    proxy = new IndexProxyImpl(dbView);

    proxy.notifyModified();
    verify(modCounter).notifyModified(eq(dbView));
  }

  @Test
  void name() {
    Snapshot dbView = createSnapshot();
    proxy = new IndexProxyImpl(dbView);

    assertThat(proxy.getName(), equalTo(INDEX_NAME));
  }

  /**
   * Create a mock of a fork.
   */
  private Fork createFork() {
    return mock(Fork.class);
  }

  /**
   * Create a mock of a snapshot.
   */
  private Snapshot createSnapshot() {
    return mock(Snapshot.class);
  }

  private static class IndexProxyImpl extends AbstractIndexProxy {

    private static final long NATIVE_HANDLE = 0x11L;

    IndexProxyImpl(View view) {
      super(new NativeHandle(NATIVE_HANDLE), INDEX_NAME, view);
    }
  }

}
