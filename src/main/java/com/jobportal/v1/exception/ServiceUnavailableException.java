package com.jobportal.v1.exception;

import org.springframework.http.HttpStatus;

public class ServiceUnavailableException extends BaseException {
    public ServiceUnavailableException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}