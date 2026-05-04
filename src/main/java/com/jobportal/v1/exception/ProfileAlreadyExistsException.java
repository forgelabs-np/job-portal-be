package com.jobportal.v1.exception;

import org.springframework.http.HttpStatus;

public class ProfileAlreadyExistsException extends BaseException {
    public ProfileAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}