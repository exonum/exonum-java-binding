package com.exonum.binding.proxy;

import static java.util.Collections.singleton;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest({
    AbstractNativeProxy.class,
})
public class ProxyPreconditionsTest {

  @Test
  public void checkValidDoesNothingIfValid() throws Exception {
    AbstractNativeProxy proxy = createProxy(true);

    ProxyPreconditions.checkValid(proxy);
  }

  @Test
  public void checkValidDoesNothingIfAllValid() throws Exception {
    Set<AbstractNativeProxy> proxies = createProxies(true, true);

    ProxyPreconditions.checkValid(proxies);
  }

  @Test(expected = IllegalStateException.class)
  public void checkValidDoesNotAcceptSingleInvalidProxy() throws Exception {
    AbstractNativeProxy proxy = createProxy(false);

    ProxyPreconditions.checkValid(proxy);
  }

  @Test(expected = IllegalStateException.class)
  public void checkValidDoesNotAcceptInvalidProxies1() throws Exception {
    Set<AbstractNativeProxy> proxies = createProxies(true, false);

    ProxyPreconditions.checkValid(proxies);
  }

  @Test(expected = IllegalStateException.class)
  public void checkValidDoesNotAcceptInvalidProxies2() throws Exception {
    Set<AbstractNativeProxy> proxies = createProxies(false, true);

    ProxyPreconditions.checkValid(proxies);
  }

  @Test(expected = NullPointerException.class)
  public void checkValidDoesNotAcceptNullProxy() throws Exception {
    Set<AbstractNativeProxy> proxies = singleton(null);

    ProxyPreconditions.checkValid(proxies);
  }

  private static Set<AbstractNativeProxy> createProxies(Boolean... isValid) {
    return Arrays.stream(isValid)
        .map(ProxyPreconditionsTest::createProxy)
        .collect(Collectors.toSet());
  }

  private static AbstractNativeProxy createProxy(boolean isValid) {
    AbstractNativeProxy p = mock(AbstractNativeProxy.class);
    when(p.isValid()).thenReturn(isValid);
    return p;
  }
}
