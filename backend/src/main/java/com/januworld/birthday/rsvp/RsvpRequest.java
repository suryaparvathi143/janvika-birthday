package com.januworld.birthday.rsvp;

import jakarta.validation.constraints.*;

public record RsvpRequest(
        @NotBlank @Size(max = 100) String guestName,
        boolean attending,
        @Min(0) @Max(20) int partySize,
        @Min(0) @Max(20) int adults,
        @Min(0) @Max(20) int toddlers,
        @Size(max = 500) String message
) {
    @AssertTrue(message = "Guest counts must total between 1 and 20 when attending")
    public boolean isPartySizeValid() {
        return !attending || (adults + toddlers >= 1 && adults + toddlers <= 20);
    }
}
