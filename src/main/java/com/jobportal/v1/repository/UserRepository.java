package com.jobportal.v1.repository;

import com.jobportal.v1.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL AND u.isActive = true")
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.accountLockedUntil = null WHERE u.email = :email")
    void resetFailedAttempts(@Param("email") String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.email = :email")
    void incrementFailedAttempts(@Param("email") String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.accountLockedUntil = :lockTime WHERE u.email = :email")
    void lockAccount(@Param("email") String email, @Param("lockTime") LocalDateTime lockTime);
}