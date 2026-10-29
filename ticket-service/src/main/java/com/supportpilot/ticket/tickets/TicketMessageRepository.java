package com.supportpilot.ticket.tickets;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {

    @EntityGraph(attributePaths = "author")
    List<TicketMessage> findByTicketIdOrderByCreatedAtAscIdAsc(Long ticketId);
}
