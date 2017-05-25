package com.bitfury.storage.DB;

import com.exonum.storage.connector.Fork;
import com.exonum.storage.connector.Snapshot;

public class LevelDB implements iDB {

	private final Object nativeLevelD;
	
	public LevelDB(String pathToDB) {
		this.nativeLevelD = nativeCreateLevelDB(pathToDB);
	}
	
	@Override
	public Object lookupSnapshot() {
		
		return new Snapshot(nativeLookupSnapshot(this.nativeLevelD));
	}

	@Override
	public Object lookupFork() {
		
		return new Fork(nativeLookupSnapshot(this.nativeLevelD));
	}

	private native Object nativeLookupSnapshot(Object nativeDB);
	private native Object nativeLookupFork(Object nativeDB);
	private native Object nativeCreateLevelDB(String path);
	
}
