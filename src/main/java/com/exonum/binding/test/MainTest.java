package com.exonum.binding.test;

import com.exonum.binding.index.IndexMap;
import com.exonum.binding.storage.connector.Connect;
import com.exonum.binding.storage.db.Database;
import com.exonum.binding.storage.db.MemoryDb;

public class MainTest {

  static {
    // To have library `libjava_bindings` available by name,
    // add a path to the folder containing it to java.library.path,
    // e.g.: java -Djava.library.path=rust/target/release …
    System.loadLibrary("java_bindings");
  }

  public static void main(String[] args) {

    TestStorageKey key = new TestStorageKey();
    TestStorageValue value = new TestStorageValue();

    Database base = new MemoryDb();

    Connect dbConnect = base.lookupFork();

    IndexMap<TestStorageKey, TestStorageValue> map =
        new IndexMap<>(TestStorageValue.class, dbConnect, null);

    map.put(key, value);

    TestStorageValue resultFromBase = map.get(key);

    System.out.println(resultFromBase.value);

    dbConnect.destroyNativeConnect();
    base.destroyNativeDb();
  }
}
