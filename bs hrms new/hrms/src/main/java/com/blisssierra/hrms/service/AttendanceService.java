package com.blisssierra.hrms.service;

import com.blisssierra.hrms.dto.FaceVerifyResponseDto;
import com.blisssierra.hrms.entity.Employee;
import com.blisssierra.hrms.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * AttendanceService
 *
 * Proxies face verification to Python FastAPI, then persists the
 * check-in / check-out record if verification succeeds.
 *
 * KEY FIX:
 * isClockedIn = true → employee is currently clocked IN → record CHECK-OUT
 * isClockedIn = false → employee is not clocked in → record CHECK-IN
 *
 * The AttendanceApiService now guarantees one row per employee per day,
 * so this service simply calls the correct method without worrying about
 * duplicate rows.
 */
@Service
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    @Value("${python.face.api.url}")
    private String pythonApiUrl;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AttendanceApiService attendanceApiService;

    /**
     * Main entry point called by AttendanceController.
     *
     * @param empId       employee ID
     * @param livePhoto   live selfie captured by the mobile / web app
     * @param isClockedIn true → currently clocked IN → this call is a CHECK-OUT
     *                    false → not clocked in → this call is a CHECK-IN
     */
    public FaceVerifyResponseDto verifyFace(String empId,
            MultipartFile livePhoto,
            boolean isClockedIn) {

        log.info("▶ verifyFace: empId={}, photo={}B, isClockedIn={}",
                empId,
                livePhoto != null ? livePhoto.getSize() : 0,
                isClockedIn);

        // ── Basic validation ─────────────────────────────────────────────────
        if (empId == null || empId.trim().isEmpty()) {
            return fail("Employee ID is required");
        }

        String normalizedEmpId = empId.trim().toUpperCase();

        Optional<Employee> optional = employeeRepository.findByEmpId(normalizedEmpId);
        if (optional.isEmpty()) {
            log.warn("Employee not found: empId={}", normalizedEmpId);
            return fail("Employee not found: " + normalizedEmpId);
        }

        Employee employee = optional.get();
        String faceImagePaths = employee.getFaceImagePaths();

        log.info("  faceImagePaths from DB: {}", faceImagePaths);

        // ── No face registered — allow but still record attendance ───────────
        if (faceImagePaths == null || faceImagePaths.trim().isEmpty()) {
            log.warn("  No face paths for empId={} — recording attendance without face check",
                    normalizedEmpId);
            persistAttendance(normalizedEmpId, isClockedIn);
            return new FaceVerifyResponseDto(true, 1.0,
                    "No registered face — attendance recorded");
        }

        // ── Photo is required when face images exist ─────────────────────────
        if (livePhoto == null || livePhoto.isEmpty()) {
            return fail("No live photo provided. Please allow camera access and try again.");
        }

        // ── Forward to Python FastAPI for face comparison ────────────────────
        FaceVerifyResponseDto result = forwardToPython(normalizedEmpId, faceImagePaths, livePhoto);

        // ── Persist attendance only on successful verification ───────────────
        if (result.isMatch()) {
            persistAttendance(normalizedEmpId, isClockedIn);
        }

        return result;
    }

    /**
     * Backward-compatible overload — defaults isClockedIn=false (check-in).
     */
    public FaceVerifyResponseDto verifyFace(String empId, MultipartFile livePhoto) {
        return verifyFace(empId, livePhoto, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Persist attendance record after successful face verification.
     *
     * isClockedIn = true → employee was clocked in → now checking OUT
     * isClockedIn = false → employee was not clocked in → now checking IN
     *
     * AttendanceApiService handles the one-record-per-day guarantee internally.
     */
    private void persistAttendance(String empId, boolean isClockedIn) {
        try {
            if (isClockedIn) {
                // Currently clocked in → record check-out
                var result = attendanceApiService.recordCheckOut(empId);
                if (result.isPresent()) {
                    log.info("  ✅ Check-out persisted for empId={}", empId);
                } else {
                    log.warn("  ⚠️  Check-out called but no open session found for empId={}", empId);
                }
            } else {
                // Not clocked in → record check-in (duplicate guard inside service)
                attendanceApiService.recordCheckIn(empId);
                log.info("  ✅ Check-in persisted for empId={}", empId);
            }
        } catch (Exception ex) {
            // Log but don't fail the face-verify response
            log.error("  ❌ Failed to persist attendance for empId={}: {}", empId, ex.getMessage(), ex);
        }
    }

    /**
     * Forward the live selfie + employee data to Python FastAPI for face
     * comparison.
     */
    private FaceVerifyResponseDto forwardToPython(String empId,
            String faceImagePaths,
            MultipartFile livePhoto) {
        String url = pythonApiUrl + "/api/face/verify";
        log.info("  Forwarding to Python: {}", url);

        try {
            RestTemplate restTemplate = new RestTemplate();

            byte[] photoBytes = livePhoto.getBytes();
            String filename = (livePhoto.getOriginalFilename() != null
                    && !livePhoto.getOriginalFilename().isEmpty())
                            ? livePhoto.getOriginalFilename()
                            : "live.jpg";

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("empId", empId);
            body.add("faceImagePaths", faceImagePaths);

            ByteArrayResource photoResource = new ByteArrayResource(photoBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };

            HttpHeaders photoHeaders = new HttpHeaders();
            photoHeaders.setContentType(MediaType.IMAGE_JPEG);
            HttpEntity<ByteArrayResource> photoPart = new HttpEntity<>(photoResource, photoHeaders);
            body.add("photo", photoPart);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<FaceVerifyResponseDto> response = restTemplate.postForEntity(url, request,
                    FaceVerifyResponseDto.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                FaceVerifyResponseDto res = response.getBody();
                log.info("  Python response: match={}, score={}, msg={}",
                        res.isMatch(), res.getScore(), res.getMessage());
                return res;
            } else {
                log.error("  Python returned non-2xx: {}", response.getStatusCode());
                return fail("Face verification service returned an error. Please try again.");
            }

        } catch (Exception e) {
            log.error("  Error calling Python FastAPI: {}", e.getMessage(), e);
            return fail(
                    "Could not reach face verification service. " +
                            "Make sure the Python server is running on " + pythonApiUrl +
                            ". Error: " + e.getMessage());
        }
    }

    private FaceVerifyResponseDto fail(String message) {
        return new FaceVerifyResponseDto(false, 0.0, message);
    }
}