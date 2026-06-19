package com.citygo.tourism.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

public class CityValidationException extends RuntimeException {
    private final String code;
    private final String field;
    private final String input;
    private final List<String> suggestions;
    private final HttpStatus status;

    public CityValidationException(String code, String message, String field, String input, List<String> suggestions) {
        this(code, message, field, input, suggestions, HttpStatus.BAD_REQUEST);
    }

    public CityValidationException(String code, String message, String field, String input, List<String> suggestions, HttpStatus status) {
        super(message);
        this.code = code;
        this.field = field;
        this.input = input;
        this.suggestions = suggestions == null ? List.of() : suggestions;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public String field() {
        return field;
    }

    public String input() {
        return input;
    }

    public List<String> suggestions() {
        return suggestions;
    }

    public HttpStatus status() {
        return status;
    }
}
