package com.exonum.binding.storage.database;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import com.exonum.binding.proxy.Cleaner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    Views.class,
})
@Ignore  // Won't run on Java 10 till Powermock is updated [ECR-1614]
public class SnapshotTest {

  @Before
  public void setUp() throws Exception {
    mockStatic(Views.class);
  }

  @Test
  public void destroy_NotOwning() throws Exception {
    try (Cleaner cleaner = new Cleaner()) {
      Snapshot.newInstance(0x0A, false, cleaner);
    }

    verifyStatic(Views.class, never());
    Views.nativeFree(anyLong());
  }

  @Test
  public void destroy_Owning() throws Exception {
    int nativeHandle = 0x0A;

    try (Cleaner cleaner = new Cleaner()) {
      Snapshot.newInstance(nativeHandle, true, cleaner);
    }

    verifyStatic(Views.class);
    Views.nativeFree(nativeHandle);
  }
}
