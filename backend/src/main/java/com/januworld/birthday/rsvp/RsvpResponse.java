package com.januworld.birthday.rsvp;

import java.time.Instant;

public record RsvpResponse(
        Long id,
        String guestName,
        boolean attending,
        int adults,
        int toddlers,
        int partySize,
        int vegetarianCount,
        int nonVegetarianCount,
        String message,
        Instant createdAt
) {
    static RsvpResponse from(Rsvp rsvp) {
        return new RsvpResponse(
                rsvp.getId(),
                rsvp.getGuestName(),
                rsvp.isAttending(),
                rsvp.getAdults(),
                rsvp.getToddlers(),
                rsvp.getPartySize(),
                rsvp.getVegetarianCount(),
                rsvp.getNonVegetarianCount(),
                rsvp.getMessage(),
                rsvp.getCreatedAt()
        );
    }
}
