package com.januworld.birthday.rsvp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RsvpRepository extends JpaRepository<Rsvp, Long> {
    List<Rsvp> findAllByOrderByCreatedAtDesc();
}
