package com.exonum.storage.DB;

import com.exonum.storage.connector.Fork;
import com.exonum.storage.connector.Snapshot;

public class MemoryDB implements DataBase {

	private final Object nativeMemoryDB;
	
	public MemoryDB() {
		this.nativeMemoryDB = nativeCreateMemoryDB();
	}
	
	@Override
	public Object lookupSnapshot() {
		
		return new Snapshot(nativeLookupSnapshot(this.nativeMemoryDB));
	}

	@Override
	public Object lookupFork() {
		
		return new Fork(nativeLookupSnapshot(this.nativeMemoryDB));
	}

	private native Object nativeLookupSnapshot(Object nativeDB);
	private native Object nativeLookupFork(Object nativeDB);
	private native Object nativeCreateMemoryDB();

}
