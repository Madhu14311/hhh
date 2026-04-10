package com.blisssierra.hrms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sent to the React Native admin screen for each attendance record.
 *
 * Times are serialised as "HH:mm" strings so the frontend can display
 * them directly without any client-side parsing.
 *
 * Example JSON:
 * {
 *   "id": 1,
 *   "empId": "EMP001",
 *   "employeeName": "Alice Smith",
 *   "date": "2025-06-10",
 *   "checkIn": "09:05",
 *   "checkOut": "17:32",
 *   "durationMinutes": 507,
 *   "status": "Present"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecordDto {

    private Long id;

    private String empId;

    private String employeeName;

    /** ISO date: "YYYY-MM-DD" */
    private String date;

    /**
     * "HH:mm" or null if the employee has not yet checked in.
     * Will always be non-null for records returned from the API because
     * records are only created on check-in.
     */
    private String checkIn;

    /**
     * "HH:mm" or null when the employee is still clocked in
     * (no check-out recorded yet).
     */
    private String checkOut;

    /**
     * Total working time in minutes, calculated server-side.
     * -1 when checkOut is null (session still open).
     */
    private long durationMinutes;

    /**
     * "Present"  — checked in, session closed
     * "Active"   — checked in, session still open
     */
    private String status;
}
