package com.smartgrade.smartgrade_backend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public String email(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new RuntimeException("No authentication");
        }

        Object principal = auth.getPrincipal();

        // Resource Server pune principal ca Jwt
        if (principal instanceof Jwt jwt) {
            // subject = email (asa l-ai setat in JwtService: subject(email))
            return jwt.getSubject();
        }

        // fallback (rar)
        String name = auth.getName();
        if (name != null && !name.isBlank()) {
            return name;
        }

        throw new RuntimeException("Cannot extract user email from JWT");
    }
}
