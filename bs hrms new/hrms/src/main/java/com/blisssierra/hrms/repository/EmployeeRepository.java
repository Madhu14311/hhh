package com.blisssierra.hrms.repository;

import com.blisssierra.hrms.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByEmpId(String empId);

    boolean existsByEmail(String email);

    boolean existsByEmpId(String empId);

    /**
     * Find by short employee code (e.g. "BSS001").
     * Sourced from Project B.
     */
    Optional<Employee> findByEmpCode(String empCode);
}