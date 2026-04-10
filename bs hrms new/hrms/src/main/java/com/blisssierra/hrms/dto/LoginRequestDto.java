package com.blisssierra.hrms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request body for POST /api/auth/login
 *
 * Sent by apiLogin() in api/authService.js:
 * {
 * "empId": "EMP001",
 * "password": "secret123"
 * }
 *
 * Note: empId is normalised to UPPERCASE in AuthService.login()
 * before the DB lookup, so the frontend can send any casing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {

    /**
     * The employee's unique ID.
     * Case-insensitive — AuthService normalises it to uppercase.
     */
    private String empId;

    /**
     * Plain-text password.
     * AuthService compares this directly against the stored value.
     * In production, replace with BCrypt: encoder.matches(raw, stored).
     */
    private String password;
}
