package com.blisssierra.hrms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Response body for POST /api/auth/upload-face
 *
 * Returned after the server saves the 3 registration selfies to disk
 * and stores their absolute paths in the employee record.
 *
 * Example success response:
 * {
 * "status": "success",
 * "message": "Face images uploaded successfully (3 photos stored)",
 * "paths": "/home/user/uploads/EMP001/face_1.jpg,..."
 * }
 *
 * Example error response:
 * {
 * "status": "error",
 * "message": "Exactly 3 face images are required. Received: 2",
 * "paths": null
 * }
 *
 * The React Native frontend checks data.status === "error" in apiUploadFace().
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceUploadResponseDto {

    /** "success" or "error" */
    private String status;

    /** Human-readable result message. */
    private String message;

    /**
     * Comma-separated absolute paths of the saved face images.
     * Null on error.
     * Example: "/home/user/hrms/uploads/faces/EMP001/face_1.jpg,
     * /home/user/hrms/uploads/faces/EMP001/face_2.jpg,
     * /home/user/hrms/uploads/faces/EMP001/face_3.jpg"
     */
    private String paths;
}
