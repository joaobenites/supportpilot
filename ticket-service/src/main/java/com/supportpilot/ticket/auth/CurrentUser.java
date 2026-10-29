package com.supportpilot.ticket.auth;

import org.springframework.security.oauth2.jwt.Jwt;

import com.supportpilot.ticket.user.Role;

/** The authenticated caller, read from the validated JWT (no database lookup per request). */
public record CurrentUser(Long id, Role role) {

    public static CurrentUser from(Jwt jwt) {
        return new CurrentUser(Long.valueOf(jwt.getSubject()), Role.valueOf(jwt.getClaimAsString(TokenService.ROLE_CLAIM)));
    }

    public boolean isAgent() {
        return role == Role.AGENT;
    }
}
