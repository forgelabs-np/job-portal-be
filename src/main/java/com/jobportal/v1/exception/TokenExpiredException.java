package com.jobportal.v1.exception;

import org.springframework.http.HttpStatus;

public class TokenExpiredException extends BaseException {
    public TokenExpiredException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}