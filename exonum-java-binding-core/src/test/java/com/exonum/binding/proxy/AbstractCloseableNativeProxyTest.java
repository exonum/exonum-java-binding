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

package com.exonum.binding.proxy;

import static com.exonum.binding.proxy.NativeHandle.INVALID_NATIVE_HANDLE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AbstractCloseableNativeProxyTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  NativeProxyFake proxy;

  @Test
  public void closeShallCallDispose() throws Exception {
    proxy = new NativeProxyFake(1L, true);
    proxy.close();
    assertThat(proxy.timesDisposed, equalTo(1));
  }

  @Test
  public void closeShallCallDisposeOnce() throws Exception {
    proxy = new NativeProxyFake(1L, true);
    proxy.close();
    proxy.close();
    assertThat(proxy.timesDisposed, equalTo(1));
  }

  @Test
  public void closeShallNotDisposeInvalidHandle() throws Exception {
    proxy = new NativeProxyFake(INVALID_NATIVE_HANDLE, true);
    proxy.close();
    assertThat(proxy.timesDisposed, equalTo(0));
  }

  @Test
  public void closeShallNotDisposeNotOwningHandle() throws Exception {
    proxy = new NativeProxyFake(1L, false);
    proxy.close();
    assertThat(proxy.timesDisposed, equalTo(0));
  }

  @Test
  public void closeShallThrowIfReferencedObjectInvalid() throws Exception {
    NativeProxyFake reference = makeProxy(2L);
    proxy = new NativeProxyFake(1L, true, reference);

    reference.close();

    expectedException.expect(IllegalStateException.class);
    proxy.close();
  }

  @Test
  public void shallBeValidOnceCreated() throws Exception {
    proxy = new NativeProxyFake(1L, true);
    assertTrue(proxy.isValidHandle());
  }

  @Test
  public void shallNotBeValidOnceClosed() throws Exception {
    proxy = new NativeProxyFake(1L, true);
    proxy.close();
    assertFalse(proxy.isValidHandle());
  }

  @Test
  public void notOwningShallNotBeValidOnceClosed() throws Exception {
    proxy = new NativeProxyFake(1L, false);
    proxy.close();
    assertFalse(proxy.isValidHandle());
  }

  @Test
  public void shallNotBeValidIfInvalidHandle() throws Exception {
    proxy = new NativeProxyFake(INVALID_NATIVE_HANDLE, true);
    assertFalse(proxy.isValidHandle());
  }

  @Test
  public void getNativeHandle() throws Exception {
    long expectedNativeHandle = 0x1FL;

    proxy = new NativeProxyFake(expectedNativeHandle, true);

    assertThat(proxy.getNativeHandle(), equalTo(expectedNativeHandle));
  }

  @Test
  public void getNativeHandle_ShallFailIfProxyIsClosed() throws Exception {
    long nativeHandle = 0x1FL;

    proxy = new NativeProxyFake(nativeHandle, true);
    proxy.close();

    expectedException.expect(IllegalStateException.class);
    proxy.getNativeHandle();  // boom
  }

  @Test
  public void getNativeHandle_ShallFailIfInvalid() throws Exception {
    long invalidHandle = 0x0L;

    proxy = new NativeProxyFake(invalidHandle, true);

    expectedException.expect(IllegalStateException.class);
    proxy.getNativeHandle();  // boom
  }

  @Test
  public void getNativeHandle_DirectlyReferencedInvalid1() throws Exception {
    long nativeHandle = 1L;
    NativeProxyFake referenced = makeProxy(20L);
    // o--x
    proxy = new NativeProxyFake(nativeHandle, true, referenced);

    assertThat(proxy.getNativeHandle(), equalTo(nativeHandle));

    referenced.close();

    assertThat(proxy, hasInvalidReferences(referenced));

    expectedException.expect(IllegalStateException.class);
    proxy.getNativeHandle();
  }

  @Test
  public void getNativeHandle_DirectlyReferencedInvalid2() throws Exception {
    long nativeHandle = 1L;
    NativeProxyFake referenced = new NativeProxyFake(20L, true, makeProxy(30L));
    // o--x--o
    proxy = new NativeProxyFake(nativeHandle, true, referenced);

    assertThat(proxy.getNativeHandle(), equalTo(nativeHandle));

    referenced.close();

    assertThat(proxy, hasInvalidReferences(referenced));

    expectedException.expect(IllegalStateException.class);
    proxy.getNativeHandle();
  }

  @Test
  public void getNativeHandle_IndirectlyReferencedInvalid1() throws Exception {
    long nativeHandle = 1L;
    NativeProxyFake referenced = makeProxy(30L);
    // o--o--x
    proxy = new NativeProxyFake(nativeHandle, true,
        new NativeProxyFake(20L, true, referenced));

    assertThat(proxy.getNativeHandle(), equalTo(nativeHandle));

    referenced.close();

    assertThat(proxy, hasInvalidReferences(referenced));

    expectedException.expect(IllegalStateException.class);
    proxy.getNativeHandle();
  }

  @Test
  public void getNativeHandle_DirectlyMultiReferenced0() throws Exception {
    long nativeHandle = 1L;
    List<AbstractCloseableNativeProxy> referenced = asList(makeProxy(20L),
        makeProxy(21L),
        makeProxy(22L)
    );
    //  /¯x
    // o--o
    //  \_o
    proxy = new NativeProxyFake(nativeHandle, true, referenced);

    assertThat(proxy.getNativeHandle(), equalTo(nativeHandle));

    referenced.get(0).close();

    assertThat(proxy, hasInvalidReferences(referenced.get(0)));

    expectedException.expect(IllegalStateException.class);
    proxy.getNativeHandle();
  }

  @Test
  public void getNativeHandle_DirectMultiReferenced1() throws Exception {
    long nativeHandle = 1L;
    List<AbstractCloseableNativeProxy> referenced = asList(makeProxy(20L),
        makeProxy(21L),
        makeProxy(22L)
    );
    //  /¯o
    // o--x
    //  \_o
    proxy = new NativeProxyFake(nativeHandle, true, referenced);

    assertThat(proxy.getNativeHandle(), equalTo(nativeHandle));

    referenced.get(1).close();

    assertThat(proxy, hasInvalidReferences(referenced.get(1)));

    expectedException.expect(IllegalStateException.class);
    proxy.getNativeHandle();
  }

  @Test
  public void getNativeHandle_DirectMultiReferenced2() throws Exception {
    long nativeHandle = 1L;
    List<AbstractCloseableNativeProxy> referenced = asList(makeProxy(20L),
        makeProxy(21L),
        makeProxy(22L)
    );
    //  /¯o
    // o--o
    //  \_x
    proxy = new NativeProxyFake(nativeHandle, true, referenced);

    assertThat(proxy.getNativeHandle(), equalTo(nativeHandle));

    referenced.get(2).close();

    assertThat(proxy, hasInvalidReferences(referenced.get(2)));

    expectedException.expect(IllegalStateException.class);
    proxy.getNativeHandle();
  }

  @Test
  public void getNativeHandle_DirectMultiReferencedAll() throws Exception {
    long nativeHandle = 1L;
    List<AbstractCloseableNativeProxy> referenced = asList(makeProxy(20L),
        makeProxy(21L),
        makeProxy(22L)
    );
    //  /¯x
    // o--x
    //  \_x
    proxy = new NativeProxyFake(nativeHandle, true, referenced);

    assertThat(proxy.getNativeHandle(), equalTo(nativeHandle));

    referenced.forEach(CloseableNativeProxy::close);

    assertThat(proxy, hasInvalidReferences(referenced));

    expectedException.expect(IllegalStateException.class);
    proxy.getNativeHandle();
  }

  @Test
  public void getNativeHandle_DiamondReferenced() throws Exception {
    long nativeHandle = 1L;

    AbstractCloseableNativeProxy referenced3 = makeProxy(30L);
    List<AbstractCloseableNativeProxy> referenced2 = asList(
        new NativeProxyFake(20L, true, referenced3),
        new NativeProxyFake(21L, true, referenced3)
    );
    // o--o--x
    //  \_o_/
    proxy = new NativeProxyFake(nativeHandle, true, referenced2);

    assertThat(proxy.getNativeHandle(), equalTo(nativeHandle));

    referenced3.close();

    assertThat(proxy, hasInvalidReferences(referenced3));

    expectedException.expect(IllegalStateException.class);
    proxy.getNativeHandle();
  }

  @Test
  public void getNativeHandle_ChainReferenced() throws Exception {
    long nativeHandle = 1L;

    AbstractCloseableNativeProxy referenced4 = makeProxy(40L);
    Collection<AbstractCloseableNativeProxy> referenced = singleton(
        new NativeProxyFake(20L, true,
            singleton(new NativeProxyFake(30L, true,
                singleton(referenced4)))));
    // o->o->o->x
    proxy = new NativeProxyFake(nativeHandle, true, referenced);

    assertThat(proxy.getNativeHandle(), equalTo(nativeHandle));

    referenced4.close();

    assertThat(proxy, hasInvalidReferences(referenced4));

    expectedException.expect(IllegalStateException.class);
    proxy.getNativeHandle();
  }

  @Test
  public void getNativeHandle_ChainReferencedAllInvalid() throws Exception {
    // o->x->x->x
    List<AbstractCloseableNativeProxy> transitivelyReferenced = new ArrayList<>();
    NativeProxyFake proxy = null;
    for (int i = 0; i < 3; i++) {
      Collection<AbstractCloseableNativeProxy> refProxy = (proxy == null)
          ? emptySet()
          : singleton(proxy);
      proxy = new NativeProxyFake((i + 1) * 10L, true, refProxy);
      transitivelyReferenced.add(proxy);
    }

    long nativeHandle = 1L;
    proxy = new NativeProxyFake(nativeHandle, true, singleton(proxy));

    assertThat(proxy.getNativeHandle(), equalTo(nativeHandle));

    // Shall destroy them in reverse order of their creation.
    for (int i = transitivelyReferenced.size() - 1; i >= 0; i--) {
      AbstractCloseableNativeProxy p = transitivelyReferenced.get(i);
      p.close();
    }

    assertThat(proxy, hasInvalidReferences(transitivelyReferenced));

    expectedException.expect(IllegalStateException.class);
    proxy.getNativeHandle();
  }

  @Test
  public void getNativeHandle_InDirectMultiReferenced1() throws Exception {
    long nativeHandle = 1L;
    List<AbstractCloseableNativeProxy> referenced3 = asList(makeProxy(30L),
        makeProxy(31L),
        makeProxy(32L)
    );
    //     /¯o
    // o--o--x
    //     \_o
    proxy = new NativeProxyFake(nativeHandle, true,
        singleton(new NativeProxyFake(20L, true, referenced3)));

    assertThat(proxy.getNativeHandle(), equalTo(nativeHandle));

    referenced3.get(1).close();

    assertThat(proxy, hasInvalidReferences(referenced3.get(1)));

    expectedException.expect(IllegalStateException.class);
    proxy.getNativeHandle();
  }

  @Test
  public void getNativeHandle_MultipleReferences() throws Exception {
    long nativeHandle = 1L;
    AbstractCloseableNativeProxy referenced2 = makeProxy(21L);
    //  /¯o
    // o   \
    //  \___x
    proxy = new NativeProxyFake(nativeHandle, true,
        asList(new NativeProxyFake(20L, true, referenced2),
            referenced2));

    assertThat(proxy.getNativeHandle(), equalTo(nativeHandle));

    referenced2.close();

    assertThat(proxy, hasInvalidReferences(referenced2));

    expectedException.expect(IllegalStateException.class);
    proxy.getNativeHandle();
  }

  private Matcher<AbstractCloseableNativeProxy> hasInvalidReferences(
      AbstractCloseableNativeProxy... expected) {
    return hasInvalidReferences(Arrays.asList(expected));
  }

  private Matcher<AbstractCloseableNativeProxy> hasInvalidReferences(
      Collection<AbstractCloseableNativeProxy> expected) {
    return new TypeSafeMatcher<AbstractCloseableNativeProxy>() {

      @Override
      protected boolean matchesSafely(AbstractCloseableNativeProxy item) {
        Set<AbstractCloseableNativeProxy> invalidReferences = item.getInvalidReferences();
        return invalidReferences.size() == expected.size()
            && invalidReferences.containsAll(expected);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a proxy with invalid references: ")
            .appendValue(expected);
      }

      @Override
      protected void describeMismatchSafely(AbstractCloseableNativeProxy item,
                                            Description mismatchDescription) {
        mismatchDescription.appendText("was a proxy with invalid references: ")
            .appendValue(item.getInvalidReferences());
      }
    };
  }

  /**
   * Creates a new disposable native proxy fake with the given handle.
   */
  private static NativeProxyFake makeProxy(long nativeHandle) {
    return new NativeProxyFake(nativeHandle, true);
  }

  private static class NativeProxyFake extends AbstractCloseableNativeProxy {

    int timesDisposed;

    final long nativeHandle;

    NativeProxyFake(long nativeHandle, boolean dispose) {
      this(nativeHandle, dispose, Collections.emptyList());
    }

    NativeProxyFake(long nativeHandle, boolean dispose,
                    AbstractCloseableNativeProxy referenced) {
      this(nativeHandle, dispose, singleton(referenced));
    }

    NativeProxyFake(long nativeHandle, boolean dispose,
                    Collection<AbstractCloseableNativeProxy> referenced) {
      super(nativeHandle, dispose, referenced);
      this.nativeHandle = nativeHandle;
      timesDisposed = 0;
    }

    @Override
    protected void disposeInternal() {
      timesDisposed++;
    }

    @Override
    public String toString() {
      return "NativeProxyFake{"
          + "nativeHandle=" + nativeHandle
          + ", timesDisposed=" + timesDisposed
          + '}';
    }
  }
}
