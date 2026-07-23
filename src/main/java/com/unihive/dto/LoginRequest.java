package com.unihive.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO carrying login credentials from the console.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /** The user's registered username. */
    private String username;

    /** The plain-text password to verify against the stored BCrypt hash. */
    private String password;
}
