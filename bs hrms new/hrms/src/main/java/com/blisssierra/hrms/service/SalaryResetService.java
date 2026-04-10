package com.blisssierra.hrms.service;

import com.blisssierra.hrms.entity.Salary;
import com.blisssierra.hrms.repository.SalaryRepository;
import com.blisssierra.hrms.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class SalaryResetService {

    private static final Logger log = LoggerFactory.getLogger(SalaryResetService.class);

    @Autowired
    private SalaryRepository salaryRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Runs at midnight on the 1st of every month.
     * Creates fresh salary records for the new month for all employees.
     * Old month records are preserved for payslip history.
     */
    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void resetMonthlySalaries() {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();
        log.info("Monthly salary reset triggered for {}/{}", month, year);

        employeeRepository.findAll().forEach(employee -> {
            // Only create if not already exists for this month
            boolean exists = salaryRepository
                    .findByEmployeeIdAndMonthAndYear(employee.getId(), month, year)
                    .isPresent();
            if (!exists) {
                Salary s = new Salary();
                s.setEmployeeId(employee.getId());
                s.setMonth(month);
                s.setYear(year);
                s.setGrossSalary(employee.getMonthlySalary()); // default gross from employee record
                s.setEarnedSalary(0);
                s.setPresentDays(0);
                salaryRepository.save(s);
                log.info("  Created fresh salary record for empId={} {}/{}", employee.getEmpId(), month, year);
            }
        });
    }
}