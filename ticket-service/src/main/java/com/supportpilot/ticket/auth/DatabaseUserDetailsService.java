package com.supportpilot.ticket.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.supportpilot.ticket.user.UserRepository;
import com.supportpilot.ticket.user.UserService;

@Service
class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    DatabaseUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return users.findByEmail(UserService.normalizeEmail(email))
                .map(SecurityUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
