package com.blisssierra.hrms.controller;

import com.blisssierra.hrms.dto.FaceVerifyResponseDto;
import com.blisssierra.hrms.service.AttendanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * AttendanceController
 *
 * CHANGE: Now accepts an optional "isClockedIn" form field (default false).
 * This tells AttendanceService whether to record a check-in or check-out.
 */
@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {

    private static final Logger log = LoggerFactory.getLogger(AttendanceController.class);

    @Autowired
    private AttendanceService attendanceService;

    /**
     * POST /api/attendance/verify-face
     *
     * Parts:
     * empId (text)
     * photo (image file)
     * isClockedIn (text, "true" or "false", optional — defaults to false)
     */
    @PostMapping(value = "/verify-face", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FaceVerifyResponseDto> verifyFace(
            @RequestParam("empId") String empId,
            @RequestParam("photo") MultipartFile photo,
            @RequestParam(value = "isClockedIn", defaultValue = "false") boolean isClockedIn) {

        log.info("POST /api/attendance/verify-face — empId={}, photo={}B, isClockedIn={}",
                empId, photo != null ? photo.getSize() : 0, isClockedIn);

        FaceVerifyResponseDto result = attendanceService.verifyFace(empId, photo, isClockedIn);

        log.info("  Result: match={}, score={}", result.isMatch(), result.getScore());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"ok\",\"service\":\"attendance\"}");
    }
}