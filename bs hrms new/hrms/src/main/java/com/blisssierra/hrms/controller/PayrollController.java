package com.blisssierra.hrms.controller;

import com.blisssierra.hrms.dto.AttendanceRecordDto;
import com.blisssierra.hrms.dto.ApiResponseDto;
import com.blisssierra.hrms.dto.SalaryResponseDto;
import com.blisssierra.hrms.service.AttendanceApiService;
import com.blisssierra.hrms.service.SalaryService;
import com.blisssierra.hrms.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * PayrollController
 *
 * Exposes Project B's attendance check-in/check-out (by empId string)
 * and salary query endpoints under /api/payroll/** to avoid conflicts
 * with Project A's existing /api/attendance/** routes.
 *
 * All business logic is delegated to AttendanceApiService (merged)
 * and SalaryService — no duplicate beans.
 *
 * Endpoints:
 * POST /api/payroll/checkin/{empId} — check in by empId string
 * POST /api/payroll/checkout/{empId} — check out by empId string
 * GET /api/payroll/salary/{employeeId} — current month salary by numeric DB id
 */
@RestController
@RequestMapping("/api/payroll")
@CrossOrigin(origins = "*")
public class PayrollController {

    private static final Logger log = LoggerFactory.getLogger(PayrollController.class);

    @Autowired
    private AttendanceApiService attendanceApiService;

    @Autowired
    private SalaryService salaryService;

    @Autowired
    private EmployeeRepository employeeRepository;

    // ── POST /api/payroll/checkin/{empId} ────────────────────────────────────
    @PostMapping("/checkin/{empId}")
    public ResponseEntity<?> checkIn(@PathVariable String empId) {
        log.info("POST /api/payroll/checkin/{}", empId);
        if (empId == null || empId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDto("error", "empId is required"));
        }
        AttendanceRecordDto record = attendanceApiService.recordCheckIn(empId);
        return ResponseEntity.ok(record);
    }

    // ── POST /api/payroll/checkout/{empId} ───────────────────────────────────
    @PostMapping("/checkout/{empId}")
    public ResponseEntity<?> checkOut(@PathVariable String empId) {
        log.info("POST /api/payroll/checkout/{}", empId);
        if (empId == null || empId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDto("error", "empId is required"));
        }
        Optional<AttendanceRecordDto> result = attendanceApiService.recordCheckOut(empId);
        if (result.isEmpty()) {
            return ResponseEntity.ok(
                    new ApiResponseDto("error",
                            "No check-in record found for today. Please check in first."));
        }
        return ResponseEntity.ok(result.get());
    }

    // ── GET /api/payroll/salary/{employeeId} ─────────────────────────────────
    /**
     * Returns current month salary for an employee by their numeric DB id (Long).
     * To look up by empId string, use GET /api/payroll/salary/by-emp/{empId}
     */
    @GetMapping("/salary/{employeeId}")
    public ResponseEntity<?> getSalaryByNumericId(@PathVariable Long employeeId) {
        log.info("GET /api/payroll/salary/{}", employeeId);
        SalaryResponseDto dto = salaryService.getCurrentMonthSalary(employeeId);
        return ResponseEntity.ok(dto);
    }

    // ── GET /api/payroll/salary/by-emp/{empId} ───────────────────────────────
    /**
     * Convenience endpoint: look up salary by empId string (e.g. "EMP001").
     * Resolves empId → numeric DB id → delegates to SalaryService.
     */
    @GetMapping("/salary/by-emp/{empId}")
    public ResponseEntity<?> getSalaryByEmpId(@PathVariable String empId) {
        log.info("GET /api/payroll/salary/by-emp/{}", empId);
        return employeeRepository.findByEmpId(empId.trim().toUpperCase())
                .map(emp -> ResponseEntity.ok(
                        (Object) salaryService.getCurrentMonthSalary(emp.getId())))
                .orElseGet(() -> ResponseEntity.badRequest()
                        .body(new ApiResponseDto("error", "Employee not found: " + empId)));
    }
}