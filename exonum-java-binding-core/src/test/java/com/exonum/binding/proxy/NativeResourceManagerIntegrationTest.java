package com.exonum.binding.proxy;

import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.util.LibraryLoader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * A couple of tests that verify that using an invalid handle from Java does not crash the VM,
 * but results in a descriptive RuntimeException.
 */
public class NativeResourceManagerIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void nativeResourceManagerShallThrowIfUnknownHandle() throws Exception {
    long unknownNativeHandle = 0x110b;
    Fork f = new Fork(unknownNativeHandle);

    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Invalid handle value: '110B'");
    f.close();
  }


  @Test
  public void nativeResourceManagerShallThrowIfHandleUsedWithOtherType() throws Exception {
    try (Database database = new MemoryDb()) {
      Fork f = new Fork(database.getNativeHandle());

      expectedException.expect(RuntimeException.class);
      expectedException.expectMessage("Wrong type id for");
      f.close();
    }
  }
}
