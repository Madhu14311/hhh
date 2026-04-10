package com.blisssierra.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body for GET /api/payroll/salary/{employeeId}
 * Wraps Salary entity data for the frontend dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryResponseDto {

    private Long id;
    private Long employeeId;
    private int month;
    private int year;
    private double grossSalary;
    private double earnedSalary;
    private int presentDays;
}