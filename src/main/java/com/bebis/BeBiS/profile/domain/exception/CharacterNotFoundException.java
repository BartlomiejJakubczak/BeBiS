package com.bebis.BeBiS.profile.domain.exception;

/**
 * No stored character matches the requested id. The web layer should answer with 404.
 */
public final class CharacterNotFoundException extends RuntimeException {
    public CharacterNotFoundException(String message) {
        super(message);
    }
}
