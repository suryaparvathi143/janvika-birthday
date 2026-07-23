package com.januworld.birthday.whatsapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class WhatsAppInvitationService {
    private final RestClient restClient;

    @Value("${app.whatsapp.enabled:false}") private boolean enabled;
    @Value("${app.whatsapp.access-token:}") private String accessToken;
    @Value("${app.whatsapp.phone-number-id:}") private String phoneNumberId;
    @Value("${app.whatsapp.template-name:birthday_invitation}") private String templateName;
    @Value("${app.whatsapp.template-language:en_US}") private String templateLanguage;
    @Value("${app.whatsapp.api-version:v23.0}") private String apiVersion;

    public WhatsAppInvitationService(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public void sendInvitation(String rawPhoneNumber) {
        if (!enabled || accessToken.isBlank() || phoneNumberId.isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "WhatsApp invitations are not configured yet");
        }
        String phoneNumber = rawPhoneNumber.replaceAll("[^0-9]", "");
        if (phoneNumber.length() < 8 || phoneNumber.length() > 15) {
            throw new ResponseStatusException(BAD_GATEWAY, "The guest phone number is invalid");
        }
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp", "to", phoneNumber, "type", "template",
                "template", Map.of("name", templateName, "language", Map.of("code", templateLanguage))
        );
        try {
            restClient.post()
                    .uri("https://graph.facebook.com/{version}/{phoneNumberId}/messages", apiVersion, phoneNumberId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_GATEWAY,
                    "WhatsApp could not send this invitation. Check the approved template and guest opt-in.");
        }
    }
}
