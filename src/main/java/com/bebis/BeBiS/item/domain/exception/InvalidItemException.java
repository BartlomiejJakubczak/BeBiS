package com.bebis.BeBiS.item.domain.exception;

/**
 * An exception that should be an indication to retry fetching of the item in question
 */

public class InvalidItemException extends RuntimeException {

    public InvalidItemException(String message) {
        super(message);
    }

}
