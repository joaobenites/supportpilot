package com.supportpilot.ticket;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/**
 * Base for integration tests: full application + real Postgres. The Spring context and the
 * container are shared by all test classes. Tests don't clean the database; instead each one
 * creates its own users with unique emails, so tests never depend on each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
abstract class IntegrationTest {

    protected static final String AGENT_EMAIL = "agent@supportpilot.test";
    protected static final String AGENT_PASSWORD = "agent-password-123";
    protected static final String PASSWORD = "s3cret-password";

    @Autowired
    protected MockMvc mvc;

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected static String userJson(String name, String email, String password) {
        return """
                {"name": "%s", "email": "%s", "password": "%s"}
                """.formatted(name, email, password);
    }

    protected long registerCustomer(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Customer", email, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    /** Registers a brand-new customer and returns its token. */
    protected String newCustomerToken() throws Exception {
        String email = uniqueEmail();
        registerCustomer(email);
        return login(email, PASSWORD);
    }

    protected String agentToken() throws Exception {
        return login(AGENT_EMAIL, AGENT_PASSWORD);
    }

    protected long createTicket(String customerToken, String title) throws Exception {
        String body = mvc.perform(post("/api/tickets")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "%s", "description": "Something is not working"}
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
