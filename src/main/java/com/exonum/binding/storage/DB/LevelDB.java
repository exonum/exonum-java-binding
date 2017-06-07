package com.exonum.binding.storage.DB;

import com.exonum.binding.storage.connector.Fork;
import com.exonum.binding.storage.connector.Snapshot;

public class LevelDB implements DataBase {

	private final long nativeLevelDB;
	
	public LevelDB(String pathToDB) {
		this.nativeLevelDB = nativeCreateLevelDB(pathToDB);
	}
	
	@Override
	public void destroyNativeDB() {
		nativeFreeLevelDB(nativeLevelDB);
	}
	
	@Override
	public Snapshot lookupSnapshot() {		
		return new Snapshot(nativeLookupSnapshot(this.nativeLevelDB));
	}

	@Override
	public Fork lookupFork() {
		return new Fork(nativeLookupFork(this.nativeLevelDB));
	}

	private native long nativeLookupSnapshot(long nativeDB);
	private native long nativeLookupFork(long nativeDB);
	private native long nativeCreateLevelDB(String path);
	private native void nativeFreeLevelDB(long nativeDB);	
}
