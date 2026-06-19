package com.citygo.tourism.dto;

public record RegisterRequest(
        String username,
        String password,
        String email,
        String phone
) {
}
