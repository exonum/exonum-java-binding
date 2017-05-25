package com.exonum.storage.DB;

public interface DataBase {

	public Object lookupSnapshot();
	public Object lookupFork();
}
