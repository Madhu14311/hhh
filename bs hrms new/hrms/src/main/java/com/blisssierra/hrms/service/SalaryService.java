package com.blisssierra.hrms.service;
import com.blisssierra.hrms.dto.SalaryResponseDto;
import com.blisssierra.hrms.entity.Salary;
import com.blisssierra.hrms.repository.SalaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Optional;
/**
 * SalaryService — exposes salary query operations.
 * Write operations (increment on checkout) are handled by AttendanceApiService
 * to keep the transaction boundary clean.
 */
@Service
public class SalaryService {
    private static final Logger log = LoggerFactory.getLogger(SalaryService.class);
    @Autowired
    private SalaryRepository salaryRepository;
    /**
     * Retrieve current month's salary for an employee by their numeric DB id.
     * Returns a zeroed-out DTO if no record exists yet for this month.
     */
    public SalaryResponseDto getCurrentMonthSalary(Long employeeId) {
        LocalDate now = LocalDate.now();
        Optional<Salary> opt = salaryRepository
                .findByEmployeeIdAndMonthAndYear(employeeId, now.getMonthValue(), now.getYear());
        Salary salary = opt.orElseGet(() -> {
            Salary s = new Salary();
            s.setEmployeeId(employeeId);
            s.setMonth(now.getMonthValue());
            s.setYear(now.getYear());
            s.setGrossSalary(0);
            s.setEarnedSalary(0);
            s.setPresentDays(0);
            return s;
        });
        return toDto(salary);
    }
    private SalaryResponseDto toDto(Salary s) {
        return new SalaryResponseDto(
                s.getId(),
                s.getEmployeeId(),
                s.getMonth(),
                s.getYear(),
                s.getGrossSalary(),
                s.getEarnedSalary(),
                s.getPresentDays());
    }
}