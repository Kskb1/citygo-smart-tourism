package com.citygo.tourism.dto;

public record UserDTO(
        long id,
        String username,
        String role,
        String email,
        String phone,
        String status
) {
}
