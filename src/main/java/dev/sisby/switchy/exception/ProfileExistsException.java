package dev.sisby.switchy.exception;

public class ProfileExistsException extends IllegalArgumentException {
    public ProfileExistsException(String profileId) {
        super(String.format("profile %s already exists!", profileId));
    }
}
