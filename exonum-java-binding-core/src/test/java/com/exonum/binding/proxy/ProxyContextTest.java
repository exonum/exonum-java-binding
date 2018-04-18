package com.exonum.binding.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.testing.NullPointerTester;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;

public class ProxyContextTest {

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  private ProxyContext context;

  @Before
  public void setUp() {
    context = new ProxyContext();
  }

  @Test
  public void testRejectsNull() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(context);
  }

  @Test
  public void addNativeProxyToClosed() throws CloseFailuresException {
    context.close();

    NativeProxy proxy = mock(NativeProxy.class);

    expectedException.expectMessage("Cannot register a proxy");
    expectedException.expect(IllegalStateException.class);
    context.add(proxy);
  }

  @Test
  public void closeEmptyNoExceptions() throws CloseFailuresException {
    context.close();
  }

  @Test
  public void closeOneProxy() throws CloseFailuresException {
    NativeProxy proxy = mock(NativeProxy.class);

    context.add(proxy);

    context.close();

    verify(proxy).close();
  }

  @Test
  public void closeMultipleProxies() throws CloseFailuresException {
    NativeProxy p1 = mock(NativeProxy.class);
    NativeProxy p2 = mock(NativeProxy.class);

    context.add(p1);
    context.add(p2);

    context.close();

    // Verify that the proxies are closed in the reversed order they were added.
    InOrder inOrder = inOrder(p2, p1);
    inOrder.verify(p2).close();
    inOrder.verify(p1).close();
  }

  @Test
  public void closeMultipleProxiesWhenFirstToBeClosedFails() {
    NativeProxy p1 = mock(NativeProxy.class);
    NativeProxy p2 = mock(NativeProxy.class);
    doThrow(RuntimeException.class).when(p2).close();

    context.add(p1);
    context.add(p2);

    try {
      context.close();
      fail("Context must report that it failed to close p2");
    } catch (CloseFailuresException e) {
      // Verify that p1 was closed.
      verify(p1).close();

      // Check the error message
      assertThat(e).hasMessageStartingWith("1 exception(s) occurred when closing this context");
    }
  }

  @Test
  public void closeIsIdempotent() throws CloseFailuresException {
    NativeProxy proxy = mock(NativeProxy.class);

    context.add(proxy);

    // First close must actually close the proxy.
    context.close();
    // Second close must not have any effects.
    context.close();

    // Verify that the proxy was closed exactly once.
    verify(proxy).close();
  }

  @Test
  public void toStringIncludesContextInformation() {
    String r = context.toString();

    assertThat(r).contains("hash");
    assertThat(r).contains("numRegisteredProxies=0");
    assertThat(r).contains("closed=false");
  }
}
