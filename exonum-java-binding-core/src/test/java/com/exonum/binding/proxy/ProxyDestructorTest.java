package com.exonum.binding.proxy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;

import java.util.function.LongConsumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    Cleaner.class,
    NativeHandle.class,
})
public class ProxyDestructorTest {

  @Test
  public void newRegistered() {
    Cleaner cleaner = mock(Cleaner.class);
    NativeHandle handle = new NativeHandle(1L);
    LongConsumer destructor = (nh) -> { };

    ProxyDestructor d = ProxyDestructor.newRegistered(cleaner, handle, destructor);

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

    ProxyDestructor d = new ProxyDestructor(handle, destructor);

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

    ProxyDestructor d = new ProxyDestructor(handle, destructor);

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
}
