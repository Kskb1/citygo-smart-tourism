package com.citygo.tourism.controller;

import com.citygo.tourism.exception.TripPayloadTooLargeException;
import com.citygo.tourism.exception.CityValidationException;
import com.citygo.tourism.exception.PlanningValidationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(CityValidationException.class)
    public ResponseEntity<Map<String, Object>> cityValidation(CityValidationException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", e.code());
        body.put("message", e.getMessage());
        body.put("field", e.field());
        body.put("input", e.input());
        body.put("suggestions", e.suggestions());
        return ResponseEntity.status(e.status()).body(body);
    }

    @ExceptionHandler(PlanningValidationException.class)
    public ResponseEntity<Map<String, Object>> planningValidation(PlanningValidationException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", e.code());
        body.put("message", e.getMessage());
        return ResponseEntity.status(e.status()).body(body);
    }

    @ExceptionHandler(TripPayloadTooLargeException.class)
    public ResponseEntity<Map<String, Object>> tripPayloadTooLarge(TripPayloadTooLargeException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "TRIP_PAYLOAD_TOO_LARGE");
        body.put("message", "行程数据过大，请精简后重新保存。");
        body.put("bytes", e.bytes());
        body.put("maxBytes", e.maxBytes());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> responseStatus(ResponseStatusException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", e.getStatusCode().toString());
        body.put("message", e.getReason() == null ? "请求处理失败。" : e.getReason());
        return ResponseEntity.status(e.getStatusCode()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "VALIDATION_FAILED");
        body.put("message", "请求参数不完整，请检查必填字段。");
        return ResponseEntity.badRequest().body(body);
    }
}
