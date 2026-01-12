package com.llms.exception;

public class InvalidStateException extends RuntimeException {
    private final String errorCode;

    public InvalidStateException(String message) {
        super(message);
        this.errorCode = "INVALID_STATE";
    }

    public InvalidStateException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
