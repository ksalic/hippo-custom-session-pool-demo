package org.onehippo.demo.security;

/**
 * @version "\$Id$" kenan
 */
public class CustomSessionPoolException extends RuntimeException {

    public CustomSessionPoolException() {
    }

    public CustomSessionPoolException(final String message) {
        super(message);
    }

    public CustomSessionPoolException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public CustomSessionPoolException(final Throwable cause) {
        super(cause);
    }

    public CustomSessionPoolException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
