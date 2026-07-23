package com.januworld.birthday.whatsapp;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "whatsapp_guests")
public class WhatsAppGuest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String guestName;
    @Column(nullable = false, unique = true, length = 25)
    private String phoneNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus invitationStatus;
    @Column(length = 500)
    private String lastError;
    private Instant sentAt;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected WhatsAppGuest() {}

    public WhatsAppGuest(String guestName, String phoneNumber) {
        this.guestName = guestName;
        this.phoneNumber = phoneNumber;
        this.invitationStatus = InvitationStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void markAccepted() {
        invitationStatus = InvitationStatus.ACCEPTED;
        lastError = null;
        sentAt = Instant.now();
    }

    public void markFailed(String error) {
        invitationStatus = InvitationStatus.FAILED;
        lastError = error.length() > 500 ? error.substring(0, 500) : error;
    }

    public Long getId() { return id; }
    public String getGuestName() { return guestName; }
    public String getPhoneNumber() { return phoneNumber; }
    public InvitationStatus getInvitationStatus() { return invitationStatus; }
    public String getLastError() { return lastError; }
    public Instant getSentAt() { return sentAt; }
    public Instant getCreatedAt() { return createdAt; }
}
