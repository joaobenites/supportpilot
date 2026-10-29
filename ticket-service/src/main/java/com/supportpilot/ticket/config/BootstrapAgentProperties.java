package com.supportpilot.ticket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** First agent account. Public sign-up only creates customers, so someone has to create the first agent. */
@ConfigurationProperties("app.bootstrap-agent")
record BootstrapAgentProperties(String name, String email, String password) {

    boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
