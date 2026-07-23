package com.januworld.birthday.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WhatsAppGuestRepository extends JpaRepository<WhatsAppGuest, Long> {
    List<WhatsAppGuest> findAllByOrderByCreatedAtAsc();
    List<WhatsAppGuest> findByInvitationStatusOrderByCreatedAtAsc(InvitationStatus invitationStatus);
    boolean existsByPhoneNumber(String phoneNumber);
}
