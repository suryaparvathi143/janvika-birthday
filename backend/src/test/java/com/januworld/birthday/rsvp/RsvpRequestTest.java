package com.januworld.birthday.rsvp;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RsvpRequestTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void attendingGuestMustIncludeAtLeastOnePerson() {
        var request = new RsvpRequest("Sam", true, 0, 0, 0, "");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void decliningGuestMayUseZeroPartySize() {
        var request = new RsvpRequest("Sam", false, 0, 0, 0, "");
        assertThat(validator.validate(request)).isEmpty();
    }
}
