package com.januworld.birthday.whatsapp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record WhatsAppInvitationRequest(
        @NotBlank(message = "A guest phone number is required")
        @Pattern(regexp = "^[+0-9() .-]{8,25}$", message = "Enter a valid phone number")
        String phoneNumber
) {}
