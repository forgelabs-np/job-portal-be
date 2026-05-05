package com.jobportal.v1.exception;

import org.springframework.http.HttpStatus;

public class AgencyPendingException extends BaseException {
    public AgencyPendingException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}