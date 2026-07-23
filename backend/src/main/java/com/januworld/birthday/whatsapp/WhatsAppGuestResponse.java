package com.januworld.birthday.whatsapp;

import java.time.Instant;

public record WhatsAppGuestResponse(Long id, String guestName, String phoneNumber,
                                    InvitationStatus invitationStatus, String lastError,
                                    Instant sentAt, Instant createdAt) {
    static WhatsAppGuestResponse from(WhatsAppGuest guest) {
        return new WhatsAppGuestResponse(guest.getId(), guest.getGuestName(), guest.getPhoneNumber(),
                guest.getInvitationStatus(), guest.getLastError(), guest.getSentAt(), guest.getCreatedAt());
    }
}
