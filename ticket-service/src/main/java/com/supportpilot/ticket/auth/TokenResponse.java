package com.supportpilot.ticket.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
