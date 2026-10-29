package com.supportpilot.ticket;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class TicketIntegrationTest extends IntegrationTest {

    @Test
    void customerCreatesTicketThatWaitsForTriage() throws Exception {
        mvc.perform(post("/api/tickets")
                        .header("Authorization", bearer(newCustomerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Cannot pay invoice", "description": "The payment page shows an error"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.triageStatus").value("PENDING"))
                .andExpect(jsonPath("$.category").doesNotExist())
                .andExpect(jsonPath("$.customer.name").value("Customer"))
                .andExpect(jsonPath("$.customer.email").doesNotExist());
    }

    @Test
    void ticketRequiresTitleAndDescription() throws Exception {
        mvc.perform(post("/api/tickets")
                        .header("Authorization", bearer(newCustomerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.description").exists());
    }

    @Test
    void agentsCannotOpenTickets() throws Exception {
        mvc.perform(post("/api/tickets")
                        .header("Authorization", bearer(agentToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"t\", \"description\": \"d\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customersOnlySeeTheirOwnTickets() throws Exception {
        String alice = newCustomerToken();
        String bob = newCustomerToken();
        long aliceTicket = createTicket(alice, "Alice ticket");
        long bobTicket = createTicket(bob, "Bob ticket");

        mvc.perform(get("/api/tickets").header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(aliceTicket));

        // 404, not 403: Alice must not even learn that Bob's ticket exists.
        mvc.perform(get("/api/tickets/{id}", bobTicket).header("Authorization", bearer(alice)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/tickets/{id}/messages", bobTicket).header("Authorization", bearer(alice)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/tickets/{id}/messages", bobTicket)
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"hi\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void agentSeesAllTicketsAndFiltersByStatus() throws Exception {
        String agent = agentToken();
        long ticketA = createTicket(newCustomerToken(), "Ticket A");
        long ticketB = createTicket(newCustomerToken(), "Ticket B");

        mvc.perform(get("/api/tickets").param("size", "100").header("Authorization", bearer(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem((int) ticketA)))
                .andExpect(jsonPath("$.content[*].id", hasItem((int) ticketB)));

        changeStatus(agent, ticketA, "RESOLVED");

        mvc.perform(get("/api/tickets").param("status", "RESOLVED").param("size", "100").header("Authorization", bearer(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status", everyItem(is("RESOLVED"))))
                .andExpect(jsonPath("$.content[*].id", hasItem((int) ticketA)))
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) ticketB))));
    }

    @Test
    void agentChangesStatusAndTakesTheTicket() throws Exception {
        long ticket = createTicket(newCustomerToken(), "Login broken");

        mvc.perform(patch("/api/tickets/{id}/status", ticket)
                        .header("Authorization", bearer(agentToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assignee.role").value("AGENT"));
    }

    @Test
    void customersCannotChangeStatus() throws Exception {
        String customer = newCustomerToken();
        long ticket = createTicket(customer, "My ticket");

        mvc.perform(patch("/api/tickets/{id}/status", ticket)
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"RESOLVED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidStatusValueIsABadRequest() throws Exception {
        long ticket = createTicket(newCustomerToken(), "Ticket");

        mvc.perform(patch("/api/tickets/{id}/status", ticket)
                        .header("Authorization", bearer(agentToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DELETED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerAndAgentExchangeMessages() throws Exception {
        String customer = newCustomerToken();
        String agent = agentToken();
        long ticket = createTicket(customer, "Need help");

        postMessage(customer, ticket, "Any news?");
        postMessage(agent, ticket, "Looking into it");

        mvc.perform(get("/api/tickets/{id}/messages", ticket).header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].content").value("Any news?"))
                .andExpect(jsonPath("$[0].author.role").value("CUSTOMER"))
                .andExpect(jsonPath("$[1].content").value("Looking into it"))
                .andExpect(jsonPath("$[1].author.role").value("AGENT"));
    }

    @Test
    void unknownSortPropertyIsABadRequest() throws Exception {
        mvc.perform(get("/api/tickets").param("sort", "password").header("Authorization", bearer(agentToken())))
                .andExpect(status().isBadRequest());
    }

    private void changeStatus(String agentToken, long ticketId, String status) throws Exception {
        mvc.perform(patch("/api/tickets/{id}/status", ticketId)
                        .header("Authorization", bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"%s\"}".formatted(status)))
                .andExpect(status().isOk());
    }

    private void postMessage(String token, long ticketId, String content) throws Exception {
        mvc.perform(post("/api/tickets/{id}/messages", ticketId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"%s\"}".formatted(content)))
                .andExpect(status().isCreated());
    }
}
