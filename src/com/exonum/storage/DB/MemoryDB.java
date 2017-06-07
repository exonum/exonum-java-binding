package com.exonum.storage.DB;

import com.exonum.storage.connector.Fork;
import com.exonum.storage.connector.Snapshot;

public class MemoryDB implements DataBase {

	private final long nativeMemoryDB;
	
	public MemoryDB() {
		this.nativeMemoryDB = nativeCreateMemoryDB();
	}
	
	@Override
	public void destroyNativeDB(){
		nativeFreeMemoryDB(nativeMemoryDB);
	}
	
	@Override
	public Snapshot lookupSnapshot() {		
		return new Snapshot(nativeLookupSnapshot(this.nativeMemoryDB));
	}

	@Override
	public Fork lookupFork() {
		return new Fork(nativeLookupFork(this.nativeMemoryDB));
	}

	private native long nativeLookupSnapshot(long nativeDB);
	private native long nativeLookupFork(long nativeDB);
	private native long nativeCreateMemoryDB();
	private native void nativeFreeMemoryDB(long nativeDB);
}
