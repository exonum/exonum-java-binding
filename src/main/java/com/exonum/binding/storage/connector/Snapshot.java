package com.exonum.binding.storage.connector;

import com.exonum.binding.storage.exception.SnapshotUsageException;

public class Snapshot implements Connect {

	private final long nativeSnapshot;
	
	public Snapshot(long snapshotObj) {
		this.nativeSnapshot = snapshotObj;
	}
	
	public long getNativeSnapshot(){
		return nativeSnapshot;
	}
	
	@Override
	public void destroyNativeConnect() {
		nativeFreeSnapshot(nativeSnapshot);
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

    // fixme(dt): no such method!
	private native void nativeFreeSnapshot(long nativeSnapshot);	
}
