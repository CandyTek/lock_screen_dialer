package com.vitbac.speeddiallocker.exceptions;

/**
 * Created by nick on 6/11/15.
 */
public class IllegalLayoutException extends RuntimeException {
    public IllegalLayoutException() {
        super();
    }

    public IllegalLayoutException(String message) {
        super(message);
    }

    public IllegalLayoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalLayoutException(Throwable cause) {
        super(cause);
    }
}
