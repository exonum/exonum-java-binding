package com.exonum.binding.proxy;

import static java.util.Collections.singleton;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest({
    AbstractNativeProxy.class,
})
public class ProxyPreconditionsTest {

  @Rule public ExpectedException expectedException = ExpectedException.none();

  private Pattern errorMessagePattern = Pattern.compile(
      "Proxy is not valid: .*AbstractNativeProxy.*", Pattern.CASE_INSENSITIVE);

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

  @Test
  public void checkValidDoesNotAcceptSingleInvalidProxy() throws Exception {
    AbstractNativeProxy proxy = createProxy(false);

    expectException();
    ProxyPreconditions.checkValid(proxy);
  }

  @Test
  public void checkValidDoesNotAcceptInvalidProxies1() throws Exception {
    Set<AbstractNativeProxy> proxies = createProxies(true, false);

    expectException();
    ProxyPreconditions.checkValid(proxies);
  }

  @Test
  public void checkValidDoesNotAcceptInvalidProxies2() throws Exception {
    Set<AbstractNativeProxy> proxies = createProxies(false, true);

    expectException();
    ProxyPreconditions.checkValid(proxies);
  }

  private void expectException() {
    expectedException.expectMessage(matchesPattern(errorMessagePattern));
    expectedException.expect(IllegalStateException.class);
  }

  @Test
  public void checkValidDoesNotAcceptNullProxy() throws Exception {
    Set<AbstractNativeProxy> proxies = singleton(null);

    expectedException.expect(NullPointerException.class);
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
