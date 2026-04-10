package com.blisssierra.hrms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "leaves")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Leave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * FK → employees.id
     *
     * FIX: Changed FetchType.LAZY → FetchType.EAGER so the employee
     * association is always loaded when a Leave is fetched from the DB.
     * This ensures Jackson can serialize employee.name / employee.empId
     * into the JSON response, which the admin screen needs to display
     * the correct employee name on leave cards.
     *
     * Previously LAZY caused the employee proxy to NOT be initialized
     * on plain findAll() calls, so Jackson serialized it as {} or null,
     * resulting in "Unknown" showing on every leave card in the admin UI.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    private LocalDate startDate;
    private LocalDate endDate;
    private int totalDays;
    private String leaveType;

    @Column(length = 1000)
    private String reason;

    /** REVIEW / APPROVED / REJECTED */
    @Column(nullable = false)
    private String status = "REVIEW";

    private LocalDate appliedDate;
    private LocalDate actionDate;
}