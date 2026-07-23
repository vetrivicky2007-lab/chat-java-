package com.unihive.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO carrying user registration input from the console.
 *
 * <p>All fields are validated by {@link com.unihive.util.InputValidator}
 * and {@link com.unihive.service.AuthService} before any database writes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /** Desired display name. Must be unique. */
    private String username;

    /** Email address. Must be unique and well-formed. */
    private String email;

    /** Plain-text password entered by the user. Hashed before storage. */
    private String password;

    /** Must exactly match {@link #password} or registration is rejected. */
    private String confirmPassword;
}
