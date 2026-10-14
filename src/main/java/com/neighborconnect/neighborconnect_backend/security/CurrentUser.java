package com.neighborconnect.neighborconnect_backend.security;

import org.springframework.security.oauth2.jwt.Jwt;

/** Small helpers for reading the logged-in user out of the JWT. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    public static boolean isAdmin(Jwt jwt) {
        return "ADMIN".equals(jwt.getClaimAsString("role"));
    }
}
