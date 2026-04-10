package com.blisssierra.hrms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request body for POST /api/auth/resend-otp
 *
 * Sent by apiResendOtp() in api/authService.js:
 * {
 * "email": "john@example.com"
 * }
 *
 * The server invalidates any existing unused OTPs for this email,
 * generates a new one, and sends it via EmailService.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResendOtpRequestDto {

    /** The employee's email address to which the new OTP will be sent. */
    private String email;
}
