package com.blisssierra.hrms.repository;

import com.blisssierra.hrms.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

  /** All records for a specific date, newest check-in first. */
  List<Attendance> findByAttendanceDateOrderByCheckInTimeDesc(LocalDate date);

  /** All records for an employee across all dates. */
  List<Attendance> findByEmpIdOrderByAttendanceDateDesc(String empId);

  /**
   * Find the single attendance record for an employee on a given date.
   * One employee = one row per day.
   */
  Optional<Attendance> findByEmpIdAndAttendanceDate(String empId, LocalDate attendanceDate);

  /**
   * Find the open (not yet checked-out) session for an employee on a given date.
   */
  @Query("""
      SELECT a FROM Attendance a
       WHERE a.empId = :empId
         AND a.attendanceDate = :date
         AND a.checkOutTime IS NULL
       ORDER BY a.checkInTime DESC
      """)
  Optional<Attendance> findOpenSession(@Param("empId") String empId,
      @Param("date") LocalDate date);

  /** All records for admin overview — most recent dates first. */
  List<Attendance> findAllByOrderByAttendanceDateDescCheckInTimeDesc();
}