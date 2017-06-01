package com.exonum.test;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.exonum.index.IndexMap;
import com.exonum.storage.DB.DataBase;
import com.exonum.storage.DB.MemoryDB;
import com.exonum.storage.connector.Connect;
import com.exonum.storage.serialization.StorageValue;

public class MainTest {

	static {
	    Path p = Paths.get("rust/target/debug/libjava_bindings.dylib");
	    System.load(p.toAbsolutePath().toString());
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
