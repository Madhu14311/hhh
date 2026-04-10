package com.blisssierra.hrms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Response body for POST /api/auth/login
 *
 * On success, all employee profile fields are returned so the React Native
 * app can populate UserContext immediately after login — no second API call
 * is needed.
 *
 * Success example:
 * {
 * "status": "success",
 * "message": "Login successful",
 * "name": "John Smith",
 * "email": "john@example.com",
 * "empId": "EMP001",
 * "designation": "Backend Developer",
 * "faceImagePaths": "/home/.../uploads/faces/EMP001/face_1.jpg,..."
 * }
 *
 * Error example:
 * {
 * "status": "error",
 * "message": "Invalid Employee ID or password",
 * "name": null,
 * "email": null,
 * "empId": null,
 * "designation": null,
 * "faceImagePaths": null
 * }
 *
 * Consumed by apiLogin() → Signin.js → UserContext.login()
 * The faceImagePaths value is stored in UserContext and later sent
 * to AttendanceService so Python FastAPI can compare the live selfie
 * against the registered face images.
 */
@Data
@NoArgsConstructor
public class LoginResponseDto {

    /** "success" or "error" */
    private String status;

    /** Human-readable message (shown on login failure). */
    private String message;

     private Long userId;

    /** Full display name of the employee. Null on error. */
    private String name;

    /** Employee's email address. Null on error. */
    private String email;

    /** Employee's unique ID (e.g. "EMP001"). Null on error. */
    private String empId;

    /** Job title / designation. Null on error. */
    private String designation;

    /**
     * Comma-separated absolute file-system paths of the 3 face images
     * captured during signup (stored by FileStorageService).
     *
     * Used by AttendanceService.verifyFace() — it reads these paths from
     * the Employee entity and forwards them to Python FastAPI so the
     * face comparison can be done against the original photos.
     *
     * Null on error or if no face images have been registered yet.
     */
    private String faceImagePaths;
}
