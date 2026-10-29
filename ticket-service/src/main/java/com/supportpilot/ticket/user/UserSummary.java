package com.supportpilot.ticket.user;

/** Public view of a user embedded in other responses (no email). */
public record UserSummary(Long id, String name, Role role) {

    public static UserSummary from(User user) {
        return user == null ? null : new UserSummary(user.getId(), user.getName(), user.getRole());
    }
}
