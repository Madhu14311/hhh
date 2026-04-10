package com.blisssierra.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API response used across all endpoints.
 *
 * Example responses:
 * { "status": "success", "message": "OTP sent to user@email.com" }
 * { "status": "error", "message": "Email already exists" }
 *
 * Mapped to/from JSON automatically by Jackson.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDto {

    /**
     * "success" or "error"
     */
    private String status;

    /**
     * Human-readable message shown to the user.
     */
    private String message;
}
