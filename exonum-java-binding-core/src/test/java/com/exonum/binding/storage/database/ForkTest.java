package com.exonum.binding.storage.database;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import com.exonum.binding.proxy.Cleaner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    Views.class,
    ViewModificationCounter.class,
})
public class ForkTest {

  private Fork fork;

  private ViewModificationCounter modCounter;

  @Before
  public void setUp() throws Exception {
    mockStatic(Views.class);
    mockStatic(ViewModificationCounter.class);
    modCounter = mock(ViewModificationCounter.class);
    when(ViewModificationCounter.getInstance()).thenReturn(modCounter);
  }

  @Test
  public void disposeInternal_OwningProxy() throws Exception {
    int nativeHandle = 0x0A;
    try (Cleaner cleaner = new Cleaner()) {
      fork = Fork.newInstance(nativeHandle, true, cleaner);
    }

    verify(modCounter).remove(fork);

    verifyStatic(Views.class);
    Views.nativeFree(nativeHandle);
  }

  @Test
  public void disposeInternal_NotOwningProxy() throws Exception {
    int nativeHandle = 0x0A;

    try (Cleaner cleaner = new Cleaner()) {
      fork = Fork.newInstance(nativeHandle, false, cleaner);
    }

    verify(modCounter).remove(fork);

    verifyStatic(Views.class, never());
    Views.nativeFree(nativeHandle);
  }

}
