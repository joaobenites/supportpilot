package com.supportpilot.ticket;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthIntegrationTest extends IntegrationTest {

    @Test
    void registerCreatesCustomerAndNeverReturnsPassword() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Ana", email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registerIgnoresRoleSentByClient() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Mallory", "email": "%s", "password": "%s", "role": "AGENT"}
                                """.formatted(uniqueEmail(), PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void registerRejectsDuplicateEmailIgnoringCase() throws Exception {
        String email = uniqueEmail();
        registerCustomer(email);
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Again", email.toUpperCase(), PASSWORD)))
                .andExpect(status().isConflict());
    }

    @Test
    void registerValidatesInput() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("", "not-an-email", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void loginReturnsBearerToken() throws Exception {
        String email = uniqueEmail();
        registerCustomer(email);
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameAnswer() throws Exception {
        String email = uniqueEmail();
        registerCustomer(email);
        for (String[] credentials : new String[][] {{email, "wrong-password"}, {uniqueEmail(), PASSWORD}}) {
            mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "%s", "password": "%s"}
                                    """.formatted(credentials[0], credentials[1])))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Invalid email or password"));
        }
    }

    @Test
    void meReturnsTheLoggedInUser() throws Exception {
        String email = uniqueEmail();
        registerCustomer(email);
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(login(email, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void protectedEndpointsRequireAToken() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        // Change the payload to claim the AGENT role but keep the original signature.
        String[] parts = newCustomerToken().split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"1\",\"role\":\"AGENT\",\"iss\":\"supportpilot-ticket-service\",\"exp\":"
                        + Instant.now().plusSeconds(3600).getEpochSecond() + "}").getBytes());
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        mvc.perform(get("/api/tickets").header("Authorization", bearer(forged)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void onlyAgentsCanCreateAgents() throws Exception {
        String newAgent = userJson("New Agent", uniqueEmail(), PASSWORD);

        mvc.perform(post("/api/users/agents")
                        .header("Authorization", bearer(newCustomerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newAgent))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/users/agents")
                        .header("Authorization", bearer(agentToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newAgent))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("AGENT"));
    }
}
