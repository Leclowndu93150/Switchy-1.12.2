package dev.sisby.switchy.exception;

public class ProfileMissingException extends IllegalArgumentException {
    public ProfileMissingException(String profileId) {
        super(String.format("profile %s doesn't exist!", profileId));
    }
}
