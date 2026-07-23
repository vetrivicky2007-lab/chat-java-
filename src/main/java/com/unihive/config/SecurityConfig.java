package com.unihive.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security configuration for UniHive.
 *
 * <p>Exposes a {@link BCryptPasswordEncoder} bean used throughout the
 * application to hash passwords on registration and verify them on login.
 * The strength factor is set to 12 (default is 10) for a good balance
 * between security and performance on modern hardware.
 *
 * <p>No Spring Security HTTP filter chain is configured here because
 * UniHive v1 is a console application without a web server.
 */
@Configuration
public class SecurityConfig {

    /** BCrypt cost factor. Increase for stronger hashing at the cost of speed. */
    private static final int BCRYPT_STRENGTH = 12;

    /**
     * Provides the {@link PasswordEncoder} bean backed by BCrypt.
     *
     * @return a {@link BCryptPasswordEncoder} with strength {@value #BCRYPT_STRENGTH}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }
}
