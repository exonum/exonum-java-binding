package com.exonum.storage.connector;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Fork implements Connect {

	private static ReadWriteLock locker = new ReentrantReadWriteLock();
	
	private final Object nativeFork;
	
	public Fork(Object forkObj) {
		this.nativeFork = forkObj;
	}
	
	public Object getNativeFork(){
		return nativeFork;
	}
	
	@Override
	public void lockWrite() {
		locker.writeLock().lock();
	}

	@Override
	public void lockRead() {
		locker.readLock().lock();
	}

	@Override
	public void unlockWrite() {
		locker.writeLock().unlock();
	}

	@Override
	public void unlockRead() {
		locker.readLock().unlock();
	}

}
