package com.vitaminbacon.lockscreendialer.exceptions;

/**
 * Created by nick on 6/11/15.
 */
public class CallHandlerException extends Exception {
    public CallHandlerException() { super(); }

    public CallHandlerException (String message) { super(message); }

    public CallHandlerException (String message, Throwable cause) { super(message, cause); }

    public CallHandlerException (Throwable cause) { super(cause); }
}
