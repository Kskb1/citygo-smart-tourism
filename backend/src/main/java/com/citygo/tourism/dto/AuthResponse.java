package com.citygo.tourism.dto;

public record AuthResponse(
        String token,
        UserDTO user
) {
}
