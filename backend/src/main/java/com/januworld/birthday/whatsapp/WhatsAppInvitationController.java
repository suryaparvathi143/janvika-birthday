package com.januworld.birthday.whatsapp;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppInvitationController {
    private final WhatsAppInvitationService invitationService;
    private final WhatsAppGuestRepository guestRepository;
    @Value("${app.whatsapp.admin-token:}") private String adminToken;

    public WhatsAppInvitationController(WhatsAppInvitationService invitationService, WhatsAppGuestRepository guestRepository) {
        this.invitationService = invitationService;
        this.guestRepository = guestRepository;
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
        invitationService.sendInvitation(request.phoneNumber(), "Guest");
        return Map.of("status", "Invitation request accepted");
    }

    @GetMapping("/guests")
    public List<WhatsAppGuestResponse> listGuests(
            @RequestHeader(value = "X-Admin-Token", defaultValue = "") String suppliedToken) {
        requireAdmin(suppliedToken);
        return guestRepository.findAllByOrderByCreatedAtAsc().stream().map(WhatsAppGuestResponse::from).toList();
    }

    @PostMapping("/guests")
    @ResponseStatus(HttpStatus.CREATED)
    public WhatsAppGuestResponse addGuest(
            @RequestHeader(value = "X-Admin-Token", defaultValue = "") String suppliedToken,
            @Valid @RequestBody WhatsAppGuestRequest request) {
        requireAdmin(suppliedToken);
        String phoneNumber = request.phoneNumber().replaceAll("[^0-9]", "");
        if (guestRepository.existsByPhoneNumber(phoneNumber)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This mobile number is already in the invitation list");
        }
        return WhatsAppGuestResponse.from(guestRepository.save(new WhatsAppGuest(request.guestName().trim(), phoneNumber)));
    }

    @PostMapping("/invitations/send-pending")
    public Map<String, Integer> sendPendingInvitations(
            @RequestHeader(value = "X-Admin-Token", defaultValue = "") String suppliedToken) {
        requireAdmin(suppliedToken);
        int accepted = 0;
        int failed = 0;
        for (WhatsAppGuest guest : guestRepository.findByInvitationStatusOrderByCreatedAtAsc(InvitationStatus.PENDING)) {
            try {
                invitationService.sendInvitation(guest.getPhoneNumber(), guest.getGuestName());
                guest.markAccepted();
                accepted++;
            } catch (ResponseStatusException exception) {
                guest.markFailed(exception.getReason() == null ? "WhatsApp failed to accept the request" : exception.getReason());
                failed++;
            }
            guestRepository.save(guest);
        }
        return Map.of("accepted", accepted, "failed", failed);
    }

    private void requireAdmin(String suppliedToken) {
        if (adminToken.isBlank() || !MessageDigest.isEqual(
                adminToken.getBytes(StandardCharsets.UTF_8), suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authorized to manage invitations");
        }
    }
}
