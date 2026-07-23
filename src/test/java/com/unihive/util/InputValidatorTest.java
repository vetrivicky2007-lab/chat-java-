package com.unihive.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class InputValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "user@gmail.com",
            "john123@yahoo.com",
            "alice.smith@outlook.com",
            "user.name+tag@sub.domain.co.uk"
    })
    void isValidEmail_shouldReturnTrueForValidEmails(String email) {
        assertTrue(InputValidator.isValidEmail(email), "Expected email to be valid: " + email);
        assertNull(InputValidator.validateEmail(email), "Expected no error message for valid email: " + email);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "usergmail.com",
            "@gmail.com",
            "user@",
            "user@.com",
            "user.com"
    })
    void isValidEmail_shouldReturnFalseForInvalidEmails(String email) {
        assertFalse(InputValidator.isValidEmail(email), "Expected email to be invalid: " + email);
        assertEquals("Invalid email format. Please enter a valid email address.", InputValidator.validateEmail(email));
    }

    @Test
    void isValidEmail_shouldHandleNullAndBlank() {
        assertFalse(InputValidator.isValidEmail(null));
        assertFalse(InputValidator.isValidEmail(""));
        assertFalse(InputValidator.isValidEmail("   "));

        assertEquals("Invalid email format. Please enter a valid email address.", InputValidator.validateEmail(null));
        assertEquals("Invalid email format. Please enter a valid email address.", InputValidator.validateEmail("  "));
    }
}
