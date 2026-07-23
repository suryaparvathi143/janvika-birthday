package com.januworld.birthday.whatsapp;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppInvitationController {
    private final WhatsAppInvitationService invitationService;
    @Value("${app.whatsapp.admin-token:}") private String adminToken;

    public WhatsAppInvitationController(WhatsAppInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> sendInvitation(
            @RequestHeader(value = "X-Admin-Token", defaultValue = "") String suppliedToken,
            @Valid @RequestBody WhatsAppInvitationRequest request) {
        if (adminToken.isBlank() || !MessageDigest.isEqual(
                adminToken.getBytes(StandardCharsets.UTF_8), suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authorized to send invitations");
        }
        invitationService.sendInvitation(request.phoneNumber());
        return Map.of("status", "Invitation request accepted");
    }
}
