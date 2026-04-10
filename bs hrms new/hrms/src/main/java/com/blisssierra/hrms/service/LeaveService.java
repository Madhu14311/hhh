package com.blisssierra.hrms.service;
import com.blisssierra.hrms.dto.LeaveRequestDTO;
import com.blisssierra.hrms.entity.Employee;
import com.blisssierra.hrms.entity.Leave;
import com.blisssierra.hrms.repository.EmployeeRepository;
import com.blisssierra.hrms.repository.LeaveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
@Service
public class LeaveService {
    private static final Logger log = LoggerFactory.getLogger(LeaveService.class);
    @Autowired
    private LeaveRepository leaveRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    public Leave applyLeave(LeaveRequestDTO dto) {
        // Look up Employee by numeric id (employees.id — the PK returned at login)
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new RuntimeException(
                        "Employee not found: id=" + dto.getEmployeeId()));
        LocalDate start = LocalDate.parse(dto.getStartDate());
        LocalDate end = LocalDate.parse(dto.getEndDate());
        int days = (int) ChronoUnit.DAYS.between(start, end) + 1;
        Leave leave = new Leave();
        leave.setEmployee(employee); // uses the Employee association, not userId
        leave.setStartDate(start);
        leave.setEndDate(end);
        leave.setTotalDays(days);
        leave.setLeaveType(dto.getLeaveType());
        leave.setReason(dto.getReason());
        leave.setStatus("REVIEW");
        leave.setAppliedDate(LocalDate.now());
        Leave saved = leaveRepository.save(leave);
        log.info("Leave applied: employeeId={}, days={}, type={}",
                dto.getEmployeeId(), days, dto.getLeaveType());
        return saved;
    }
    public List<Leave> getUserLeaves(Long employeeId) {
        return leaveRepository.findByEmployeeId(employeeId);
    }
    public List<Leave> getPendingLeaves() {
        return leaveRepository.findByStatus("REVIEW");
    }
    public List<Leave> getAllLeaves() {
        return leaveRepository.findAll();
    }
    public Leave updateStatus(Long id, String status) {
        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave not found: id=" + id));
        leave.setStatus(status);
        leave.setActionDate(LocalDate.now());
        Leave updated = leaveRepository.save(leave);
        log.info("Leave id={} updated to status={}", id, status);
        return updated;
    }
}