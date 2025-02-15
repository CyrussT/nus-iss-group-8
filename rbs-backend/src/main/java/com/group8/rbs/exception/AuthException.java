package com.group8.rbs.exception;

import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}
