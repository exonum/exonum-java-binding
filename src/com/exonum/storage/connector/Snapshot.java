package com.exonum.storage.connector;

import com.exonum.storage.exception.SnapshotUsageException;

public class Snapshot implements Connect {

	private final Object nativeSnapshot;
	
	public Snapshot(Object snapshotObj) {
		this.nativeSnapshot = snapshotObj;
	}
	
	public Object getNativeSnapshot(){
		return nativeSnapshot;
	}
	
	@Override
	public void lockWrite() {
		throw new SnapshotUsageException();
	}

	@Override
	public void lockRead() {
		//method do nothing for Snapshot
	}

	@Override
	public void unlockWrite() {
		throw new SnapshotUsageException();
	}

	@Override
	public void unlockRead() {
		//method do nothing for Snapshot
	}

}
