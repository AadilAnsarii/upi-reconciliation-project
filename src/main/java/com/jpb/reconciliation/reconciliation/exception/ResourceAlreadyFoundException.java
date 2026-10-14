package com.jpb.reconciliation.reconciliation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FOUND)
public class ResourceAlreadyFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ResourceAlreadyFoundException(String message) {
//		super(String.format("%s found with the given input data %s: '%s'", resouceName, filedName, fieldValue));
		super(message);
	}

}
