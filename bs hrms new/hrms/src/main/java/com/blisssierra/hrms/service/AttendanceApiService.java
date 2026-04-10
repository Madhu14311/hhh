package com.blisssierra.hrms.service;

import com.blisssierra.hrms.dto.AttendanceRecordDto;
import com.blisssierra.hrms.entity.Attendance;
import com.blisssierra.hrms.entity.Employee;
import com.blisssierra.hrms.entity.Salary;
import com.blisssierra.hrms.repository.AttendanceRepository;
import com.blisssierra.hrms.repository.EmployeeRepository;
import com.blisssierra.hrms.repository.SalaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AttendanceApiService (merged)
 *
 * Responsibilities:
 * 1. One-row-per-day check-in / check-out (Project A logic — preserved)
 * 2. Salary update on successful check-out (Project B logic — absorbed)
 *
 * Project B's AttendanceService is NOT a separate bean — its salary logic
 * lives here to avoid a duplicate service and circular dependency.
 */
@Service
public class AttendanceApiService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceApiService.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Per-day salary increment sourced from Project B.
     * Should ideally be driven by Employee.monthlySalary / working days.
     * Kept as a constant to preserve Project B's existing behaviour.
     */
    private static final double PER_DAY_AMOUNT = 612.0;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SalaryRepository salaryRepository;

    // ── CHECK-IN ─────────────────────────────────────────────────────────────

    /**
     * Records a check-in for the given employee.
     *
     * - Duplicate guard: if a record already exists for (empId + today), returns
     * it.
     * - Sets present=true on the new record (Project B requirement).
     */
    @Transactional
    public AttendanceRecordDto recordCheckIn(String empId) {
        String normId = empId.trim().toUpperCase();
        LocalDate today = LocalDate.now();
        log.info("▶ recordCheckIn: empId={} date={}", normId, today);

        Optional<Attendance> existing = attendanceRepository.findByEmpIdAndAttendanceDate(normId, today);
        if (existing.isPresent()) {
            log.info("  ℹ️  Duplicate check-in prevented for empId={} on {}", normId, today);
            return toDto(existing.get());
        }

        String name = employeeRepository.findByEmpId(normId)
                .map(Employee::getName)
                .orElse(normId);

        Attendance record = new Attendance();
        record.setEmpId(normId);
        record.setEmployeeName(name);
        record.setAttendanceDate(today);
        record.setCheckInTime(LocalDateTime.now());
        record.setPresent(true); // Project B field

        Attendance saved = attendanceRepository.save(record);
        log.info("  ✅ Check-in saved id={} at {}", saved.getId(), saved.getCheckInTime());
        return toDto(saved);
    }

    // ── CHECK-OUT ────────────────────────────────────────────────────────────

    /**
     * Records a check-out for the given employee.
     *
     * - Finds the existing record for (empId + today).
     * - Updates checkOutTime on the same row (no new row).
     * - Triggers salary update (Project B logic).
     */
    @Transactional
    public Optional<AttendanceRecordDto> recordCheckOut(String empId) {
        String normId = empId.trim().toUpperCase();
        LocalDate today = LocalDate.now();
        log.info("▶ recordCheckOut: empId={} date={}", normId, today);

        Optional<Attendance> existingOpt = attendanceRepository.findByEmpIdAndAttendanceDate(normId, today);

        if (existingOpt.isEmpty()) {
            log.warn("  ⚠️  No attendance record found for empId={} on {}", normId, today);
            return Optional.empty();
        }

        Attendance record = existingOpt.get();
        record.setCheckOutTime(LocalDateTime.now());
        Attendance saved = attendanceRepository.save(record);

        long durationMinutes = ChronoUnit.MINUTES.between(
                saved.getCheckInTime(), saved.getCheckOutTime());
        log.info("  ✅ Check-out saved id={} duration={}min", saved.getId(), durationMinutes);

        // Trigger salary update after successful checkout (Project B logic)
        updateSalaryOnCheckOut(normId);

        return Optional.of(toDto(saved));
    }

    // ── QUERIES ──────────────────────────────────────────────────────────────

    public List<AttendanceRecordDto> getByDate(LocalDate date) {
        return attendanceRepository
                .findByAttendanceDateOrderByCheckInTimeDesc(date)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<AttendanceRecordDto> getAll() {
        return attendanceRepository
                .findAllByOrderByAttendanceDateDescCheckInTimeDesc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ── SALARY LOGIC (absorbed from Project B) ───────────────────────────────

    /**
     * Updates or creates the monthly salary record for an employee
     * after a successful check-out.
     *
     * Logic sourced from Project B's AttendanceService.updateSalary().
     * Key change: empId is now String (Project A authority).
     * Salary.employeeId is resolved from Employee.id (Long) via lookup.
     */
    private void updateSalaryOnCheckOut(String empId) {
        try {
            Employee employee = employeeRepository.findByEmpId(empId).orElse(null);
            if (employee == null) {
                log.warn("  ⚠️  updateSalary: employee not found for empId={}", empId);
                return;
            }

            Long employeeNumericId = employee.getId();
            LocalDate now = LocalDate.now();
            int month = now.getMonthValue();
            int year = now.getYear();

            Salary salary = salaryRepository
                    .findByEmployeeIdAndMonthAndYear(employeeNumericId, month, year)
                    .orElseGet(() -> {
                        Salary s = new Salary();
                        s.setEmployeeId(employeeNumericId);
                        s.setMonth(month);
                        s.setYear(year);
                        // Seed grossSalary from Employee.monthlySalary if set
                        s.setGrossSalary(employee.getMonthlySalary());
                        s.setEarnedSalary(0);
                        s.setPresentDays(0);
                        return s;
                    });

            salary.setPresentDays(salary.getPresentDays() + 1);
            salary.setEarnedSalary(salary.getEarnedSalary() + PER_DAY_AMOUNT);
            salaryRepository.save(salary);

            log.info("  💰 Salary updated for empId={}: presentDays={}, earned={}",
                    empId, salary.getPresentDays(), salary.getEarnedSalary());

        } catch (Exception ex) {
            // Non-fatal — log but don't fail the check-out response
            log.error("  ❌ Salary update failed for empId={}: {}", empId, ex.getMessage(), ex);
        }
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private AttendanceRecordDto toDto(Attendance a) {
        String checkIn = a.getCheckInTime() != null ? a.getCheckInTime().format(TIME_FMT) : null;
        String checkOut = a.getCheckOutTime() != null ? a.getCheckOutTime().format(TIME_FMT) : null;

        long durationMinutes = -1;
        if (a.getCheckInTime() != null && a.getCheckOutTime() != null) {
            durationMinutes = ChronoUnit.MINUTES.between(
                    a.getCheckInTime(), a.getCheckOutTime());
        }

        String status = (a.getCheckOutTime() == null) ? "Active" : "Present";

        return new AttendanceRecordDto(
                a.getId(),
                a.getEmpId(),
                a.getEmployeeName(),
                a.getAttendanceDate().format(DATE_FMT),
                checkIn,
                checkOut,
                durationMinutes,
                status);
    }
}