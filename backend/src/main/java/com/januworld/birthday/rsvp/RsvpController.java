package com.januworld.birthday.rsvp;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/rsvps")
public class RsvpController {
    private final RsvpRepository repository;

    public RsvpController(RsvpRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody RsvpRequest request) {
        Rsvp saved = repository.save(new Rsvp(
                request.guestName().trim(),
                request.attending(),
                request.adults(),
                request.toddlers(),
                request.message() == null ? "" : request.message().trim()
        ));
        return Map.of("id", saved.getId(), "attending", saved.isAttending());
    }

    @GetMapping
    public List<RsvpResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(RsvpResponse::from)
                .toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "RSVP not found");
        }
        repository.deleteById(id);
    }
}
