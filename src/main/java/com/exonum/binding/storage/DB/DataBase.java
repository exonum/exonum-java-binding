package com.exonum.binding.storage.DB;

import com.exonum.binding.storage.connector.Fork;
import com.exonum.binding.storage.connector.Snapshot;

public interface DataBase {

	public Snapshot lookupSnapshot();
	public Fork lookupFork();
	
	public void destroyNativeDB();
}
