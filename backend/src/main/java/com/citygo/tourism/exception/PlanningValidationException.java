package com.citygo.tourism.exception;

import org.springframework.http.HttpStatus;

public class PlanningValidationException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public PlanningValidationException(String code, String message) {
        super(message);
        this.code = code;
        this.status = HttpStatus.BAD_REQUEST;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }
}
