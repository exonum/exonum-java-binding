package com.exonum.storage.exception;

public class NotImplementedMethodException extends RuntimeException {

	public NotImplementedMethodException(){
		super("You have to implement deserializeFromRaw method in StorageValue implementer class.");
	}
}
