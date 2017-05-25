package com.exonum.storage.exception;

public class StorageOperationException extends RuntimeException {

	public StorageOperationException(){
		super("Problem while using storage operation occurred");
	}
}
