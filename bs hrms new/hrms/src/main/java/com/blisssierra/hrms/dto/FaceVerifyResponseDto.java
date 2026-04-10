package com.blisssierra.hrms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Response body for POST /api/attendance/verify-face
 *
 * This DTO is used in two directions:
 * 1. Returned by AttendanceService.forwardToPython() after deserialising
 * the JSON response from Python FastAPI (/api/face/verify).
 * 2. Returned directly by AttendanceController to the React Native app.
 *
 * Python FastAPI returns:
 * {
 * "match": true | false,
 * "score": 0.7234,
 * "message": "Face verified successfully"
 * }
 *
 * The mobile app (Attendance.js → apiFaceVerify()) reads:
 * result.match → true/false to decide check-in/out
 * result.score → displayed for debugging
 * result.message → shown to the user on failure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceVerifyResponseDto {

    /**
     * true → live selfie matched a registered face image
     * false → no match or an error occurred
     *
     * @JsonProperty("match") ensures Jackson maps the Python response field
     * "match" (lowercase) correctly even though the Java getter is isMatch().
     */
    @JsonProperty("match")
    private boolean match;

    /**
     * Best similarity score computed by Python FastAPI.
     * Range: 0.0 (no similarity) → 1.0 (identical).
     * The current threshold in main.py is 0.58.
     */
    private double score;

    /**
     * Human-readable result message from Python FastAPI, e.g.:
     * "Face verified successfully"
     * "Face mismatch (score=0.423, threshold=0.58). Please ensure good lighting..."
     * "Could not process live photo: Cannot decode image bytes"
     */
    private String message;
}
