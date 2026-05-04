package com.jobportal.v1.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BaseException {
        public NotFoundException(String message) {
            super(message, HttpStatus.NOT_FOUND);
        }
    }