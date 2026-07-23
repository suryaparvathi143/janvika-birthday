package com.januworld.birthday.rsvp;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rsvps")
public class RsvpController {
    private final RsvpRepository repository;

    public RsvpController(RsvpRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody RsvpRequest request) {
        List<String> matchingNames = repository.findAll().stream()
                .map(Rsvp::getGuestName)
                .filter(existingName -> namesMayMatch(request.guestName(), existingName))
                .distinct()
                .toList();
        if (!request.confirmDuplicate() && !matchingNames.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "code", "POSSIBLE_DUPLICATE",
                    "matches", matchingNames
            ));
        }

        Rsvp saved = repository.save(new Rsvp(
                request.guestName().trim(),
                request.attending(),
                request.adults(),
                request.toddlers(),
                request.vegetarianCount(),
                request.nonVegetarianCount(),
                request.message() == null ? "" : request.message().trim()
        ));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", saved.getId(), "attending", saved.isAttending()));
    }

    @GetMapping
    public List<RsvpResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(RsvpResponse::from)
                .toList();
    }

    @GetMapping("/names")
    public List<String> listGuestNames() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(Rsvp::getGuestName)
                .distinct()
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

    private boolean namesMayMatch(String submittedName, String existingName) {
        Set<String> submittedWords = nameWords(submittedName);
        Set<String> existingWords = nameWords(existingName);
        return submittedWords.stream().anyMatch(existingWords::contains);
    }

    private Set<String> nameWords(String name) {
        return Arrays.stream(name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
                .filter(word -> word.length() >= 2)
                .collect(Collectors.toSet());
    }
}
