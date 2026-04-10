package com.blisssierra.hrms.repository;

import com.blisssierra.hrms.entity.Salary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, Long> {

    Optional<Salary> findByEmployeeIdAndMonthAndYear(Long employeeId, int month, int year);
}