package com.blisssierra.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One attendance record per employee per calendar day.
 * Merged from Project A (face-verify flow) + Project B (salary trigger flow).
 */
@Entity
@Table(name = "attendance_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK: employee's unique string ID (e.g. "EMP001"). Project A authority. */
    @Column(name = "emp_id", nullable = false)
    private String empId;

    /** Denormalised display name — avoids JOIN in list queries. */
    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    /** Calendar date of this attendance event. */
    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    /** Timestamp when employee checked in. */
    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    /**
     * Timestamp when employee checked out.
     * Null while session is still open.
     */
    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    /**
     * Whether the employee completed a present day.
     * Sourced from Project B — set to true on check-in, used by salary logic.
     */
    @Column(name = "present", nullable = false)
    private boolean present = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}