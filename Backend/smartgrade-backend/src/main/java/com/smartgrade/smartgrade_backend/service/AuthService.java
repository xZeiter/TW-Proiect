package com.smartgrade.smartgrade_backend.service;

import com.smartgrade.smartgrade_backend.dto.auth.AuthResponse;
import com.smartgrade.smartgrade_backend.entity.UserEntity;
import com.smartgrade.smartgrade_backend.repository.UserRepository;
import com.smartgrade.smartgrade_backend.security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(String email, String password) {
        if (email == null || email.isBlank()) throw new RuntimeException("Email is required");
        if (password == null || password.isBlank()) throw new RuntimeException("Password is required");
        if (userRepository.existsByEmail(email)) throw new RuntimeException("Email already used");

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(user.getId(), user.getEmail(), token);
    }

    public AuthResponse login(String email, String password) {
        if (email == null || email.isBlank()) throw new RuntimeException("Email is required");
        if (password == null || password.isBlank()) throw new RuntimeException("Password is required");

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(user.getId(), user.getEmail(), token);
    }
}
