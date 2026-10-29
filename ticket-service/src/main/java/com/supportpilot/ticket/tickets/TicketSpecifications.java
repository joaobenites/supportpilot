package com.supportpilot.ticket.tickets;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.supportpilot.ticket.tickets.TicketDtos.TicketFilter;

/** Builds the WHERE clause of the ticket list from the filters that were actually sent. */
final class TicketSpecifications {

    private TicketSpecifications() {
    }

    static Specification<Ticket> matching(Long customerId, TicketFilter filter) {
        List<Specification<Ticket>> specs = new ArrayList<>();
        if (customerId != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId));
        }
        if (filter.status() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
        }
        if (filter.category() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("category"), filter.category()));
        }
        if (filter.priority() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("priority"), filter.priority()));
        }
        return Specification.allOf(specs);
    }
}
