package com.januworld.birthday.rsvp;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "rsvps")
public class Rsvp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String guestName;
    @Column(nullable = false)
    private boolean attending;
    @Column(nullable = false)
    private int partySize;
    @Column(nullable = false)
    private int adults;
    @Column(nullable = false)
    private int toddlers;
    @Column(length = 500)
    private String message;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Rsvp() {}

    public Rsvp(String guestName, boolean attending, int adults, int toddlers, String message) {
        this.guestName = guestName;
        this.attending = attending;
        this.adults = attending ? adults : 0;
        this.toddlers = attending ? toddlers : 0;
        this.partySize = this.adults + this.toddlers;
        this.message = message;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getGuestName() { return guestName; }
    public boolean isAttending() { return attending; }
    public int getPartySize() { return partySize; }
    public int getAdults() { return adults; }
    public int getToddlers() { return toddlers; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
