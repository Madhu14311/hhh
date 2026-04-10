package com.blisssierra.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")  // ✅ maps to the correct DB column
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "emp_id", nullable = false, unique = true)
    private String empId;

    @Column(nullable = false)
    private String designation;

    @Column(nullable = false)
    private String password;

    @Column(name = "face_image_paths", columnDefinition = "TEXT")
    private String faceImagePaths;

    @Column(name = "is_verified", nullable = false)
    private boolean verified = false;

    @Column(name = "emp_code")
    private String empCode;

    @Column(name = "monthly_salary")
    private double monthlySalary;

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