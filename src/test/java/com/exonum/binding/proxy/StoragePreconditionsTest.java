package com.exonum.binding.proxy;

import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
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
public class StoragePreconditionsTest {

  @Rule ExpectedException expected = ExpectedException.none();

  @Test
  public void checkStoragePrefixAcceptsNonEmpty() throws Exception {
    byte[] prefix = new byte[]{'p'};

    assertThat(prefix, sameInstance(StoragePreconditions.checkIndexPrefix(prefix)));
  }

  @Test(expected = NullPointerException.class)
  public void checkStoragePrefixDoesNotAcceptNull() throws Exception {
    StoragePreconditions.checkIndexPrefix(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkStoragePrefixDoesNotAcceptEmpty() throws Exception {
    byte[] prefix = new byte[]{};

    StoragePreconditions.checkIndexPrefix(prefix);
  }

  @Test
  public void checkStorageKeyAcceptsEmpty() throws Exception {
    byte[] key = new byte[]{};

    assertThat(key, sameInstance(StoragePreconditions.checkStorageKey(key)));
  }

  @Test
  public void checkStorageKeyAcceptsNonEmpty() throws Exception {
    byte[] key = new byte[]{'k'};

    assertThat(key, sameInstance(StoragePreconditions.checkStorageKey(key)));
  }

  @Test(expected = NullPointerException.class)
  public void checkStorageKeyDoesNotAcceptNull() throws Exception {
    StoragePreconditions.checkStorageKey(null);
  }

  @Test
  public void checkStorageValueAcceptsEmpty() throws Exception {
    byte[] value = new byte[]{};

    assertThat(value, sameInstance(StoragePreconditions.checkStorageValue(value)));
  }

  @Test
  public void checkStorageValueAcceptsNonEmpty() throws Exception {
    byte[] value = new byte[]{'v'};

    assertThat(value, sameInstance(StoragePreconditions.checkStorageValue(value)));
  }

  @Test
  public void checkValidDoesNothingIfValid() throws Exception {
    AbstractNativeProxy proxy = createProxy(true);

    StoragePreconditions.checkValid(proxy);
  }

  @Test
  public void checkValidDoesNothingIfAllValid() throws Exception {
    Set<AbstractNativeProxy> proxies = createProxies(true, true);

    StoragePreconditions.checkValid(proxies);
  }

  @Test(expected = NullPointerException.class)
  public void checkStorageValueDoesNotAcceptNull() throws Exception {
    StoragePreconditions.checkStorageKey(null);
  }

  @Test(expected = IllegalStateException.class)
  public void checkValidDoesNotAcceptSingleInvalidProxy() throws Exception {
    AbstractNativeProxy proxy = createProxy(false);

    StoragePreconditions.checkValid(proxy);
  }

  @Test(expected = IllegalStateException.class)
  public void checkValidDoesNotAcceptInvalidProxies1() throws Exception {
    Set<AbstractNativeProxy> proxies = createProxies(true, false);

    StoragePreconditions.checkValid(proxies);
  }

  @Test(expected = IllegalStateException.class)
  public void checkValidDoesNotAcceptInvalidProxies2() throws Exception {
    Set<AbstractNativeProxy> proxies = createProxies(false, true);

    StoragePreconditions.checkValid(proxies);
  }

  private Set<AbstractNativeProxy> createProxies(Boolean... isValid) {
    return Arrays.stream(isValid)
        .map(StoragePreconditionsTest::createProxy)
        .collect(Collectors.toSet());
  }

  private static AbstractNativeProxy createProxy(boolean isValid) {
    AbstractNativeProxy p = mock(AbstractNativeProxy.class);
    when(p.isValid()).thenReturn(isValid);
    return p;
  }

  @Test(expected = NullPointerException.class)
  public void checkValidDoesNotAcceptNullProxy() throws Exception {
    Set<AbstractNativeProxy> proxies = singleton(null);

    StoragePreconditions.checkValid(proxies);
  }

  @Test
  public void checkCanModifyThrowsIfSnapshotPassed() throws Exception {
    Snapshot dbView = mock(Snapshot.class);

    expected.expect(UnsupportedOperationException.class);
    Pattern pattern = Pattern.compile("Cannot modify the view: .*[Ss]napshot.*"
            + "\\nUse a Fork to modify any collection\\.", Pattern.MULTILINE);
    expected.expectMessage(matchesPattern(pattern));
    StoragePreconditions.checkCanModify(dbView);
  }

  @Test
  public void checkCanModifyThrowsIfNullPassed() throws Exception {
    View dbView = null;

    expected.expect(UnsupportedOperationException.class);
    expected.expectMessage("Cannot modify the view: null"
        + "\nUse a Fork to modify any collection.");
    StoragePreconditions.checkCanModify(dbView);
  }

  @Test
  public void checkCanModifyAcceptsFork() throws Exception {
    View dbView = mock(Fork.class);

    StoragePreconditions.checkCanModify(dbView);
  }

  @Test
  public void checkElementIndexNegative() throws Exception {
    expected.expect(IndexOutOfBoundsException.class);
    expected.expectMessage("Index must be in range [0, 2), but: -1");

    StoragePreconditions.checkElementIndex(-1, 2);
  }

  @Test
  public void checkElementIndexEqualToSize() throws Exception {
    expected.expect(IndexOutOfBoundsException.class);
    expected.expectMessage("Index must be in range [0, 2), but: 2");

    StoragePreconditions.checkElementIndex(2, 2);
  }

  @Test
  public void checkElementIndexGreaterThanSize() throws Exception {
    expected.expect(IndexOutOfBoundsException.class);
    expected.expectMessage("Index must be in range [0, 2), but: 3");

    StoragePreconditions.checkElementIndex(3, 2);
  }

  @Test
  public void checkElementIndexMaxLong() throws Exception {
    expected.expect(IndexOutOfBoundsException.class);
    expected.expectMessage("Index must be in range [0, 2), but:");

    StoragePreconditions.checkElementIndex(Long.MAX_VALUE, 2);
  }

  @Test
  public void checkElementIndex0MinValid() throws Exception {
    long index = 0;
    long size = 3;

    assertThat(StoragePreconditions.checkElementIndex(index, size), equalTo(index));
  }

  @Test
  public void checkElementIndex1() throws Exception {
    long index = 1;
    long size = 3;

    assertThat(StoragePreconditions.checkElementIndex(index, size), equalTo(index));
  }

  @Test
  public void checkElementIndex2MaxValid() throws Exception {
    long index = 2;
    long size = 3;

    assertThat(StoragePreconditions.checkElementIndex(index, size), equalTo(index));
  }
}
