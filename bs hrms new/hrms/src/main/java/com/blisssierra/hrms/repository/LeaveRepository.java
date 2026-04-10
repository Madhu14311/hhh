package com.blisssierra.hrms.repository;

import com.blisssierra.hrms.entity.Leave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {

    // Correct: navigate through the employee association
    List<Leave> findByEmployeeId(Long employeeId);

    List<Leave> findByStatus(String status);
}