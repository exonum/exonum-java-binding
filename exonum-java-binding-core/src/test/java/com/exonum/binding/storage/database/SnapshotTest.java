package com.exonum.binding.storage.database;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    Views.class,
})
public class SnapshotTest {

  @Before
  public void setUp() throws Exception {
    mockStatic(Views.class);
  }

  @Test
  public void disposeInternal_NotOwning() throws Exception {
    Snapshot snapshot = new Snapshot(0x0A, false);

    snapshot.close();

    verifyStatic(Views.class, never());
    Views.nativeFree(anyLong());
  }

  @Test
  public void disposeInternal_Owning() throws Exception {
    int nativeHandle = 0x0A;
    Snapshot snapshot = new Snapshot(nativeHandle, true);

    snapshot.close();

    verifyStatic(Views.class);
    Views.nativeFree(nativeHandle);
  }
}
