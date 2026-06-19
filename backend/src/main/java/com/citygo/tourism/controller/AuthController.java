package com.citygo.tourism.controller;

import com.citygo.tourism.dto.AuthRequest;
import com.citygo.tourism.dto.AuthResponse;
import com.citygo.tourism.dto.RegisterRequest;
import com.citygo.tourism.dto.UserDTO;
import com.citygo.tourism.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public UserDTO register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/profile")
    public UserDTO profile(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return authService.profile(authorization);
    }
}
