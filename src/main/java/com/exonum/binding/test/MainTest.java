package com.exonum.binding.test;

import com.exonum.binding.index.IndexMap;
import com.exonum.binding.storage.DB.DataBase;
import com.exonum.binding.storage.DB.MemoryDB;
import com.exonum.binding.storage.connector.Connect;

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

		DataBase base = new MemoryDB();
		
		Connect dbConnect = base.lookupFork();
		
		IndexMap<TestStorageKey, TestStorageValue> map = new IndexMap<TestStorageKey, TestStorageValue>(TestStorageValue.class, dbConnect, null);
		
		map.put(key, value);
		
		TestStorageValue resultFromBase = (TestStorageValue)map.get(key);
		
		System.out.println(resultFromBase.value);
		
		dbConnect.destroyNativeConnect();
		base.destroyNativeDB();
	}
}
