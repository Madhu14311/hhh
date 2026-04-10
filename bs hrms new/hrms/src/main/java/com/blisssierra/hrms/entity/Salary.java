package com.blisssierra.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Monthly salary record per employee.
 * earnedSalary increments by PER_DAY_AMOUNT on each checkout.
 * Sourced from Project B.
 */
@Entity
@Table(name = "salaries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Salary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * References Employee.id (Long PK) — consistent with Project A's Employee.
     * Project B used Long empId directly; kept as Long for salary isolation.
     */
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false)
    private int year;

    /** Full monthly CTC (e.g. 19000). */
    private double grossSalary;

    /** Accumulated earned salary this month (presentDays × perDayAmount). */
    private double earnedSalary;

    /** Count of days employee was present this month. */
    private int presentDays;
}
