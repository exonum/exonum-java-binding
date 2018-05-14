package com.exonum.binding.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.function.LongConsumer;
import org.junit.Test;

public class ProxyDestructorTest {

  @Test
  public void newRegistered() {
    Cleaner cleaner = mock(Cleaner.class);
    NativeHandle handle = new NativeHandle(1L);
    LongConsumer destructor = (nh) -> { };

    ProxyDestructor d = ProxyDestructor.newRegistered(cleaner, handle, CloseableNativeProxy.class,
        destructor);

    // Check the destructor is not null.
    assertNotNull(d);

    // Check it was added to the cleaner.
    verify(cleaner).add(d);
  }

  @Test
  public void clean() {
    long rawNativeHandle = 1L;
    NativeHandle handle = new NativeHandle(rawNativeHandle);
    LongConsumer destructor = mock(LongConsumer.class);

    ProxyDestructor d = newDestructor(handle, destructor);

    d.clean();

    // Check the handle is no longer valid.
    assertFalse(handle.isValid());

    // Check that destructor was called.
    verify(destructor).accept(rawNativeHandle);
  }

  @Test
  public void cleanIdempotent() {
    long rawNativeHandle = 1L;
    NativeHandle handle = spy(new NativeHandle(rawNativeHandle));
    LongConsumer destructor = mock(LongConsumer.class);

    ProxyDestructor d = newDestructor(handle, destructor);

    // Clean multiple times.
    int attemptsToClean = 3;
    for (int i = 0; i < attemptsToClean; i++) {
      d.clean();
    }

    assertFalse(handle.isValid());

    // Verify that the expected interactions happened exactly once.
    verify(handle).close();
    verify(destructor).accept(rawNativeHandle);
  }

  @Test
  public void getResourceType() {
    NativeHandle handle = new NativeHandle(1L);
    Class<?> proxyClass = CloseableNativeProxy.class;
    LongConsumer destructor = mock(LongConsumer.class);

    ProxyDestructor d = new ProxyDestructor(handle, proxyClass, destructor);

    assertThat(d.resourceType()).hasValue(proxyClass);
  }

  private static ProxyDestructor newDestructor(NativeHandle handle, LongConsumer destructor) {
    return new ProxyDestructor(handle, Object.class, destructor);
  }
}
