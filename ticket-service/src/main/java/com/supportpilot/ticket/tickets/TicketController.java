package com.supportpilot.ticket.tickets;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.supportpilot.ticket.auth.CurrentUser;
import com.supportpilot.ticket.common.PageResponse;
import com.supportpilot.ticket.tickets.TicketDtos.ChangeStatusRequest;
import com.supportpilot.ticket.tickets.TicketDtos.CreateTicketRequest;
import com.supportpilot.ticket.tickets.TicketDtos.MessageResponse;
import com.supportpilot.ticket.tickets.TicketDtos.NewMessageRequest;
import com.supportpilot.ticket.tickets.TicketDtos.TicketFilter;
import com.supportpilot.ticket.tickets.TicketDtos.TicketResponse;

@RestController
@RequestMapping("/api/tickets")
class TicketController {

    private final TicketService ticketService;

    TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    ResponseEntity<TicketResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateTicketRequest request,
            UriComponentsBuilder uri) {
        TicketResponse ticket = ticketService.create(CurrentUser.from(jwt), request);
        return ResponseEntity.created(uri.path("/api/tickets/{id}").build(ticket.id())).body(ticket);
    }

    @GetMapping
    PageResponse<TicketResponse> list(@AuthenticationPrincipal Jwt jwt, TicketFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ticketService.list(CurrentUser.from(jwt), filter, pageable);
    }

    @GetMapping("/{id}")
    TicketResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ticketService.get(CurrentUser.from(jwt), id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('AGENT')")
    TicketResponse changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request) {
        return ticketService.changeStatus(CurrentUser.from(jwt), id, request.status());
    }

    @GetMapping("/{id}/messages")
    List<MessageResponse> listMessages(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ticketService.listMessages(CurrentUser.from(jwt), id);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    MessageResponse addMessage(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody NewMessageRequest request) {
        return ticketService.addMessage(CurrentUser.from(jwt), id, request.content());
    }
}
