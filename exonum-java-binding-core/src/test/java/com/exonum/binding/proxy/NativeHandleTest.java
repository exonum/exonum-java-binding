package com.exonum.binding.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NativeHandleTest {

  private NativeHandle nativeHandle;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void getIfValid() {
    long handle = 0x11L;
    nativeHandle = new NativeHandle(handle);

    assertTrue(nativeHandle.isValid());
    assertThat(nativeHandle.get()).isEqualTo(handle);
  }

  @Test
  public void getInvalid() {
    long handle = NativeHandle.INVALID_NATIVE_HANDLE;
    nativeHandle = new NativeHandle(handle);

    assertFalse(nativeHandle.isValid());

    expectedException.expect(IllegalStateException.class);
    nativeHandle.get();
  }

  @Test
  public void close() {
    long handle = 0x11L;
    nativeHandle = new NativeHandle(handle);

    nativeHandle.close();
    assertFalse(nativeHandle.isValid());
  }

  @Test
  public void closeMultipleTimes() {
    long handle = 0x11L;
    nativeHandle = new NativeHandle(handle);

    nativeHandle.close();
    nativeHandle.close();
    assertFalse(nativeHandle.isValid());
  }
}
