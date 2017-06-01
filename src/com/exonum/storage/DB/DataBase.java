package com.exonum.storage.DB;

import com.exonum.storage.connector.Fork;
import com.exonum.storage.connector.Snapshot;

public interface DataBase {

	public Snapshot lookupSnapshot();
	public Fork lookupFork();
	
	public void destroyNativeDB();
}
