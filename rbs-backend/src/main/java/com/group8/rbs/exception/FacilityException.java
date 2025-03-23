package com.group8.rbs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class FacilityException extends RuntimeException {
    public FacilityException(String message) {
        super(message);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND) 
    public static class FacilityNotFoundException extends FacilityException {
        public FacilityNotFoundException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT) 
    public static class FacilityAlreadyExistsException extends FacilityException {
        public FacilityAlreadyExistsException(String message) {
            super(message);
        }
    }
}
