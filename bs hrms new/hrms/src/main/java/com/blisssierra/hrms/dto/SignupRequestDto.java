package com.blisssierra.hrms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request body for POST /api/auth/signup
 *
 * Sent by apiSignup() in api/authService.js:
 * {
 * "name": "John Smith",
 * "email": "john@example.com",
 * "empId": "EMP001",
 * "designation": "Software Developer",
 * "password": "secret123"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequestDto {

    /** Full name of the employee. */
    private String name;

    /** Work / personal email address. */
    private String email;

    /** Unique employee ID (e.g. EMP001). */
    private String empId;

    /** Job title / designation (e.g. "Backend Developer"). */
    private String designation;

    /**
     * Plain-text password.
     * NOTE: In production replace with BCrypt hashing via
     * Spring Security's PasswordEncoder.
     */
    private String password;
}