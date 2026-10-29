package com.supportpilot.ticket.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload to create a user. There is intentionally no "role" field: the endpoint decides the role,
 * so a client can never promote itself to AGENT by adding a field to the JSON.
 */
public record NewUserRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 254) String email,
        // BCrypt only uses the first 72 bytes of a password.
        @NotBlank @Size(min = 8, max = 72) String password) {
}
