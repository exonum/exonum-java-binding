package com.exonum.test;

import com.exonum.index.IndexMap;
import com.exonum.storage.DB.DataBase;
import com.exonum.storage.DB.MemoryDB;
import com.exonum.storage.connector.Connect;
import com.exonum.storage.serialization.StorageValue;

public class MainTest {

	public static void main(String[] args) {
		
		TestStorageKey key = new TestStorageKey();
		TestStorageValue value = new TestStorageValue();

		DataBase base = new MemoryDB();
		
		Connect dbConnect = (Connect) base.lookupFork();
		
		IndexMap<TestStorageKey, TestStorageValue> map = new IndexMap<TestStorageKey, TestStorageValue>(TestStorageValue.class, dbConnect, null);
		
		map.put(key, value);
		
		StorageValue resultFromBase = map.get(key);
		
	}

}
