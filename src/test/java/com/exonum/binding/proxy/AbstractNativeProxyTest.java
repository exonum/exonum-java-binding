package com.exonum.binding.proxy;

import static com.exonum.binding.proxy.AbstractNativeProxy.INVALID_NATIVE_HANDLE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AbstractNativeProxyTest {

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
  public void shallBeValidOnceCreated() throws Exception {
    proxy = new NativeProxyFake(1L, true);
    assertTrue(proxy.isValid());
  }

  @Test
  public void shallNotBeValidOnceClosed() throws Exception {
    proxy = new NativeProxyFake(1L, true);
    proxy.close();
    assertFalse(proxy.isValid());
  }

  @Test
  public void shallNotBeValidIfInvalidHandle() throws Exception {
    proxy = new NativeProxyFake(INVALID_NATIVE_HANDLE, true);
    assertFalse(proxy.isValid());
  }

  private static class NativeProxyFake extends AbstractNativeProxy {

    int timesDisposed;

    NativeProxyFake(long nativeHandle, boolean owningHandle) {
      super(nativeHandle, owningHandle);
      timesDisposed = 0;
    }

    @Override
    void disposeInternal() {
      timesDisposed++;
    }
  }
}
