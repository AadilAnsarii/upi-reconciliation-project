package com.jpb.reconciliation.reconciliation.exception;

public class KalUserCustomException extends RuntimeException {

    public KalUserCustomException(String message) {
        super(message);
    }

    public KalUserCustomException(String message, Throwable cause) {
        super(message, cause);
    }
}