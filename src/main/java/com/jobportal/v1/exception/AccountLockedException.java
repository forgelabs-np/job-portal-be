package com.jobportal.v1.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends BaseException {
    public AccountLockedException(String message) {
        super(message, HttpStatus.LOCKED);
    }
}