package com.exonum.binding.proxy;

import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.util.LibraryLoader;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * A couple of tests that verify that using an invalid handle from Java does not crash the VM,
 * but results in a descriptive RuntimeException.
 */
@Ignore // fixme: either use log messages to assert, or add a private API to MemoryDb/Views/etc.
public class NativeResourceManagerIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void nativeResourceManagerShallThrowIfUnknownForkHandle() throws Exception {
    long unknownNativeHandle = 0x110B;

    try (Cleaner cleaner = new Cleaner()) {
      Fork f = Fork.newInstance(unknownNativeHandle, cleaner);

      expectedException.expect(RuntimeException.class);
      expectedException.expectMessage("Invalid handle value: '110B'");
    }
  }

  @Test
  public void nativeResourceManagerShallThrowIfUnknownSnapshotHandle() throws Exception {
    long unknownNativeHandle = 0xABCD;
    try (Cleaner cleaner = new Cleaner()) {
      Snapshot s = Snapshot.newInstance(unknownNativeHandle, cleaner);

      expectedException.expect(RuntimeException.class);
      expectedException.expectMessage("Invalid handle value: 'ABCD'");
    }
  }

  @Test
  public void nativeResourceManagerShallThrowIfHandleUsedWithOtherType() throws Exception {
    try (Database database = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork f = database.createFork(cleaner);

      // Try to use a handle to fork to access a snapshot.
      Snapshot s = Snapshot.newInstance(f.getNativeHandle(), cleaner);

      expectedException.expect(RuntimeException.class);
      expectedException.expectMessage("Wrong type id for");
    }
  }
}
