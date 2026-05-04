package com.jobportal.v1.repository;

import com.jobportal.v1.entity.PasswordResetLog;
import com.jobportal.v1.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetLogRepository extends JpaRepository<PasswordResetLog, Long> {
    Optional<PasswordResetLog> findTopByUserOrderByRequestedAtDesc(User user);
}