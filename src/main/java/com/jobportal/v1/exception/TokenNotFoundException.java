package com.jobportal.v1.exception;

import org.springframework.http.HttpStatus;

public class TokenNotFoundException extends BaseException {
    public TokenNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}