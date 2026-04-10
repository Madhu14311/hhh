package com.blisssierra.hrms.repository;

import com.blisssierra.hrms.entity.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpRecord, Long> {

    /**
     * Find the latest unused OTP for a given email.
     */
    Optional<OtpRecord> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    /**
     * Mark all OTPs for a given email as used (cleanup).
     */
    @Modifying
    @Transactional
    @Query("UPDATE OtpRecord o SET o.used = true WHERE o.email = :email")
    void markAllUsedByEmail(@Param("email") String email);
}
