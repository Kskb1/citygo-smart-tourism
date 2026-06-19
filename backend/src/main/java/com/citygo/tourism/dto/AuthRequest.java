package com.citygo.tourism.dto;

public record AuthRequest(
        String username,
        String password
) {
}
