package com.blisssierra.hrms.controller;

import com.blisssierra.hrms.dto.*;
import com.blisssierra.hrms.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**
 * AuthController — maps exactly to the URLs in api/config.js ENDPOINTS.
 *
 * POST /api/auth/signup → signup()
 * POST /api/auth/upload-face → uploadFace()
 * POST /api/auth/verify-otp → verifyOtp()
 * POST /api/auth/resend-otp → resendOtp()
 * POST /api/auth/login → login()
 *
 * CROSS-PLATFORM MULTIPART FIX (uploadFace):
 * ─────────────────────────────────────────────
 * Web (browser): React Native Web's FormData sends each image as a
 * proper File/Blob part. The part name is "images" (set in authService.js).
 *
 * Mobile (Expo Go): React Native fetch sends {uri, name, type} which becomes
 * a standard multipart file part named "images".
 *
 * Problem: on some request configurations Spring may see the parts under
 * different iterator keys. This controller uses HttpServletRequest to
 * manually collect ALL parts named "images" (or any part that is a file),
 * making it resilient to platform differences.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    @Autowired
    private AuthService authService;
    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/auth/signup
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/signup")
    public ResponseEntity<ApiResponseDto> signup(@RequestBody SignupRequestDto req) {
        log.info("POST /api/auth/signup — empId={}, email={}", req.getEmpId(), req.getEmail());
        ApiResponseDto response = authService.signup(req);
        HttpStatus status = "success".equals(response.getStatus()) ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/auth/upload-face
    // Content-Type: multipart/form-data
    // Parts:
    // email (text)
    // images (file) × 3 ← name MUST match formData.append("images", ...) in
    // authService.js
    //
    // Uses HttpServletRequest directly so we can collect ALL file parts
    // regardless of how the browser/RN client serialised the multipart body.
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping(value = "/upload-face", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FaceUploadResponseDto> uploadFace(HttpServletRequest request) {
        if (!(request instanceof MultipartHttpServletRequest multipartRequest)) {
            log.error("POST /api/auth/upload-face — not a multipart request");
            return ResponseEntity.badRequest()
                    .body(new FaceUploadResponseDto("error", "Request must be multipart/form-data", null));
        }
        // Extract email from form fields
        String email = multipartRequest.getParameter("email");
        log.info("POST /api/auth/upload-face — email={}", email);
        // Collect ALL file parts (handles both "images" and any other name used by the
        // client)
        List<MultipartFile> images = new ArrayList<>();
        // Primary: look for parts explicitly named "images"
        MultiValueMap<String, MultipartFile> fileMap = multipartRequest.getMultiFileMap();
        if (fileMap.containsKey("images")) {
            List<MultipartFile> namedImages = fileMap.get("images");
            if (namedImages != null) {
                for (MultipartFile f : namedImages) {
                    if (f != null && !f.isEmpty()) {
                        images.add(f);
                    }
                }
            }
        }
        // Fallback: if no "images" parts found, collect ALL non-empty file parts
        if (images.isEmpty()) {
            log.warn("  No parts named 'images' found — scanning all multipart file entries...");
            for (Map.Entry<String, List<MultipartFile>> entry : fileMap.entrySet()) {
                for (MultipartFile f : entry.getValue()) {
                    if (f != null && !f.isEmpty()) {
                        log.info("  Found file part named '{}': {} bytes", entry.getKey(), f.getSize());
                        images.add(f);
                    }
                }
            }
        }
        log.info("  Total valid image parts collected: {}", images.size());
        FaceUploadResponseDto response = authService.uploadFace(email, images);
        HttpStatus status = "success".equals(response.getStatus()) ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/auth/verify-otp
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponseDto> verifyOtp(@RequestBody OtpVerifyRequestDto req) {
        log.info("POST /api/auth/verify-otp — email={}", req.getEmail());
        ApiResponseDto response = authService.verifyOtp(req);
        HttpStatus status = "success".equals(response.getStatus()) ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/auth/resend-otp
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponseDto> resendOtp(@RequestBody ResendOtpRequestDto req) {
        log.info("POST /api/auth/resend-otp — email={}", req.getEmail());
        ApiResponseDto response = authService.resendOtp(req);
        HttpStatus status = "success".equals(response.getStatus()) ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/auth/login
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto req) {
        log.info("POST /api/auth/login — empId={}", req.getEmpId());
        LoginResponseDto response = authService.login(req);
        HttpStatus status = "success".equals(response.getStatus()) ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(response);
    }
    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/auth/health
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<ApiResponseDto> health() {
        return ResponseEntity.ok(new ApiResponseDto("success", "Auth service is running"));
    }
}
