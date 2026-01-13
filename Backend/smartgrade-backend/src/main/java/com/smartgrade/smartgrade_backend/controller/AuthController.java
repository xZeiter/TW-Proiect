package com.smartgrade.smartgrade_backend.controller;

import com.smartgrade.smartgrade_backend.dto.auth.*;
import com.smartgrade.smartgrade_backend.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req.email, req.password));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req.email, req.password));
    }
}
