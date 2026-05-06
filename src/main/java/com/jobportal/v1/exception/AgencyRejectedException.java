package com.jobportal.v1.exception;

import org.springframework.http.HttpStatus;

public class AgencyRejectedException extends BaseException {
    public AgencyRejectedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}