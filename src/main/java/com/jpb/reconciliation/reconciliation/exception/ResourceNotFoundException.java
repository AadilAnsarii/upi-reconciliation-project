package com.jpb.reconciliation.reconciliation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ResourceNotFoundException(String msg) {
//		String resourceName, String fieldName, Long fieldValue
//		super(String.format("%s not found with the given input data %s : '%s'", resourceName, fieldName, fieldValue));
		super(msg);
	}
}
