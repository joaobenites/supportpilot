package com.supportpilot.ticket.user;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.supportpilot.ticket.common.ConflictException;
import com.supportpilot.ticket.common.NotFoundException;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User create(NewUserRequest request, Role role) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("Email already in use");
        }
        try {
            return users.saveAndFlush(new User(request.name().trim(), email, passwordEncoder.encode(request.password()), role));
        } catch (DataIntegrityViolationException e) {
            // Two concurrent sign-ups with the same email: the unique index decides.
            throw new ConflictException("Email already in use");
        }
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
