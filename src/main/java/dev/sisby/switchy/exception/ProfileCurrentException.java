package dev.sisby.switchy.exception;

public class ProfileCurrentException extends IllegalArgumentException {
    public ProfileCurrentException(String profileId) {
        super(String.format("%s is the current profile! switch first", profileId));
    }
}
