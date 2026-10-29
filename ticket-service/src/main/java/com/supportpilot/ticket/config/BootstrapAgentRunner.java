package com.supportpilot.ticket.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.supportpilot.ticket.user.NewUserRequest;
import com.supportpilot.ticket.user.Role;
import com.supportpilot.ticket.user.UserRepository;
import com.supportpilot.ticket.user.UserService;

@Component
class BootstrapAgentRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAgentRunner.class);

    private final BootstrapAgentProperties properties;
    private final UserRepository users;
    private final UserService userService;

    BootstrapAgentRunner(BootstrapAgentProperties properties, UserRepository users, UserService userService) {
        this.properties = properties;
        this.users = users;
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isConfigured() || users.existsByEmail(UserService.normalizeEmail(properties.email()))) {
            return;
        }
        String name = properties.name() == null || properties.name().isBlank() ? "Agent" : properties.name();
        userService.create(new NewUserRequest(name, properties.email(), properties.password()), Role.AGENT);
        log.info("Created bootstrap agent account {}", properties.email());
    }
}
