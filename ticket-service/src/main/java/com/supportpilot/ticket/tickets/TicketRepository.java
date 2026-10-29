package com.supportpilot.ticket.tickets;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    // The entity graphs load customer and assignee in the same query, avoiding one extra query per ticket (N+1).

    @Override
    @EntityGraph(attributePaths = {"customer", "assignee"})
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "assignee"})
    Optional<Ticket> findWithUsersById(Long id);

    @EntityGraph(attributePaths = {"customer", "assignee"})
    Optional<Ticket> findWithUsersByIdAndCustomerId(Long id, Long customerId);
}
