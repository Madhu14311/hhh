package com.blisssierra.hrms.controller;

import com.blisssierra.hrms.dto.LeaveRequestDTO;
import com.blisssierra.hrms.entity.Leave;
import com.blisssierra.hrms.service.LeaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave")
@CrossOrigin(origins = "*")
public class LeaveController {

    private static final Logger log = LoggerFactory.getLogger(LeaveController.class);

    @Autowired
    private LeaveService leaveService;

    @PostMapping("/apply")
    public ResponseEntity<Leave> applyLeave(@RequestBody LeaveRequestDTO dto) {
        log.info("POST /api/leave/apply employeeId={}", dto.getEmployeeId());
        return ResponseEntity.ok(leaveService.applyLeave(dto));
    }

    // Path variable stays "userId" so the frontend URL doesn't change
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Leave>> getUserLeaves(@PathVariable Long userId) {
        log.info("GET /api/leave/user/{}", userId);
        return ResponseEntity.ok(leaveService.getUserLeaves(userId));
    }

    @GetMapping("/admin/pending")
    public ResponseEntity<List<Leave>> getPendingLeaves() {
        return ResponseEntity.ok(leaveService.getPendingLeaves());
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<Leave>> getAllLeaves() {
        return ResponseEntity.ok(leaveService.getAllLeaves());
    }

    @PutMapping("/admin/approve/{id}")
    public ResponseEntity<Leave> approveLeave(@PathVariable Long id) {
        return ResponseEntity.ok(leaveService.updateStatus(id, "APPROVED"));
    }

    @PutMapping("/admin/reject/{id}")
    public ResponseEntity<Leave> rejectLeave(@PathVariable Long id) {
        return ResponseEntity.ok(leaveService.updateStatus(id, "REJECTED"));
    }
}