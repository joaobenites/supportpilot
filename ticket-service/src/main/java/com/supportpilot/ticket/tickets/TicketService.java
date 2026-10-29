package com.supportpilot.ticket.tickets;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.supportpilot.ticket.auth.CurrentUser;
import com.supportpilot.ticket.common.NotFoundException;
import com.supportpilot.ticket.common.PageResponse;
import com.supportpilot.ticket.tickets.TicketDtos.CreateTicketRequest;
import com.supportpilot.ticket.tickets.TicketDtos.MessageResponse;
import com.supportpilot.ticket.tickets.TicketDtos.TicketFilter;
import com.supportpilot.ticket.tickets.TicketDtos.TicketResponse;
import com.supportpilot.ticket.user.User;
import com.supportpilot.ticket.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository tickets;
    private final TicketMessageRepository messages;
    private final UserRepository users;

    TicketService(TicketRepository tickets, TicketMessageRepository messages, UserRepository users) {
        this.tickets = tickets;
        this.messages = messages;
        this.users = users;
    }

    @Transactional
    public TicketResponse create(CurrentUser customer, CreateTicketRequest request) {
        User owner = users.getReferenceById(customer.id());
        Ticket ticket = tickets.save(new Ticket(owner, request.title().trim(), request.description().trim()));
        return TicketResponse.from(ticket);
    }

    /** Agents see every ticket; customers only their own, whatever filters they send. */
    public PageResponse<TicketResponse> list(CurrentUser user, TicketFilter filter, Pageable pageable) {
        Long customerId = user.isAgent() ? null : user.id();
        return PageResponse.of(tickets.findAll(TicketSpecifications.matching(customerId, filter), pageable), TicketResponse::from);
    }

    public TicketResponse get(CurrentUser user, Long ticketId) {
        return TicketResponse.from(findVisible(user, ticketId));
    }

    @Transactional
    public TicketResponse changeStatus(CurrentUser agent, Long ticketId, TicketStatus status) {
        Ticket ticket = findVisible(agent, ticketId);
        ticket.changeStatus(status, users.getReferenceById(agent.id()));
        tickets.flush(); // run the UPDATE now so updatedAt in the response matches the database
        return TicketResponse.from(ticket);
    }

    public List<MessageResponse> listMessages(CurrentUser user, Long ticketId) {
        Ticket ticket = findVisible(user, ticketId);
        return messages.findByTicketIdOrderByCreatedAtAscIdAsc(ticket.getId()).stream().map(MessageResponse::from).toList();
    }

    @Transactional
    public MessageResponse addMessage(CurrentUser user, Long ticketId, String content) {
        Ticket ticket = findVisible(user, ticketId);
        TicketMessage message = messages.save(new TicketMessage(ticket, users.getReferenceById(user.id()), content.trim()));
        return MessageResponse.from(message);
    }

    /**
     * A customer asking for someone else's ticket gets 404, not 403: a 403 would confirm the ticket
     * exists. Filtering by owner in the query itself also means we can't forget the check later.
     */
    private Ticket findVisible(CurrentUser user, Long ticketId) {
        var ticket = user.isAgent()
                ? tickets.findWithUsersById(ticketId)
                : tickets.findWithUsersByIdAndCustomerId(ticketId, user.id());
        return ticket.orElseThrow(() -> new NotFoundException("Ticket not found"));
    }
}
