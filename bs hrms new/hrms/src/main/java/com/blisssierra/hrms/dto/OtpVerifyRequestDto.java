package com.blisssierra.hrms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request body for POST /api/auth/verify-otp
 *
 * Sent by apiVerifyOtp() in api/authService.js:
 * {
 * "email": "john@example.com",
 * "otp": "482910"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyRequestDto {

    /** The employee's email address (used to look up the OTP record). */
    private String email;

    /**
     * The 6-digit OTP code entered by the user in the StepOtp screen
     * (auth/Signup.js → StepOtp component).
     */
    private String otp;
}
