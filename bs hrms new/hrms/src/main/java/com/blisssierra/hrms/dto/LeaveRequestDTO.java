package com.blisssierra.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestDTO {
    private Long employeeId; // maps to employees.id — the numeric PK
    private String startDate;
    private String endDate;
    private String leaveType;
    private String reason;
}