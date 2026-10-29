package com.supportpilot.ticket.tickets;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.supportpilot.ticket.user.UserSummary;

/** Request and response shapes of the tickets API. Entities are never exposed directly. */
public final class TicketDtos {

    private TicketDtos() {
    }

    public record CreateTicketRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 5000) String description) {
    }

    public record ChangeStatusRequest(@NotNull TicketStatus status) {
    }

    public record NewMessageRequest(@NotBlank @Size(max = 5000) String content) {
    }

    /** Optional query-string filters for the ticket list; null means "any". */
    public record TicketFilter(TicketStatus status, Category category, Priority priority) {
    }

    public record TicketResponse(
            Long id,
            String title,
            String description,
            TicketStatus status,
            Category category,
            Priority priority,
            Sentiment sentiment,
            TriageStatus triageStatus,
            UserSummary customer,
            UserSummary assignee,
            Instant createdAt,
            Instant updatedAt) {

        static TicketResponse from(Ticket ticket) {
            return new TicketResponse(
                    ticket.getId(),
                    ticket.getTitle(),
                    ticket.getDescription(),
                    ticket.getStatus(),
                    ticket.getCategory(),
                    ticket.getPriority(),
                    ticket.getSentiment(),
                    ticket.getTriageStatus(),
                    UserSummary.from(ticket.getCustomer()),
                    UserSummary.from(ticket.getAssignee()),
                    ticket.getCreatedAt(),
                    ticket.getUpdatedAt());
        }
    }

    public record MessageResponse(Long id, UserSummary author, String content, Instant createdAt) {

        static MessageResponse from(TicketMessage message) {
            return new MessageResponse(message.getId(), UserSummary.from(message.getAuthor()), message.getContent(), message.getCreatedAt());
        }
    }
}
