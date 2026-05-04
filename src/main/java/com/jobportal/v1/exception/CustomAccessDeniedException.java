package com.jobportal.v1.exception;

import org.springframework.http.HttpStatus;

public class CustomAccessDeniedException extends BaseException {
    public CustomAccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}