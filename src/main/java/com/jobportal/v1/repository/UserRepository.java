package com.jobportal.v1.repository;

import com.jobportal.v1.entity.User;
import com.jobportal.v1.enums.ApprovalStatus;
import com.jobportal.v1.enums.RoleEnum;
import com.jobportal.v1.repository.projection.AgencyAggregateStatsProjection;
import com.jobportal.v1.repository.projection.ApprovalStatusCountProjection;
import com.jobportal.v1.repository.projection.DailyAgencyCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    List<User> findByRolesContaining(@Param("role") RoleEnum role);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    Page<User> findByRolesContaining(@Param("role") RoleEnum role, Pageable pageable);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role AND u.approvalStatus = :status")
    List<User> findByRolesContainingAndApprovalStatus(@Param("role") RoleEnum role, @Param("status") ApprovalStatus status);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role AND u.approvalStatus = :status")
    Page<User> findByRolesContainingAndApprovalStatus(@Param("role") RoleEnum role, @Param("status") ApprovalStatus status, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r = :role")
    Long countByRole(@Param("role") RoleEnum role);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r = :role AND u.approvalStatus = :status")
    Long countByRoleAndApprovalStatus(@Param("role") RoleEnum role, @Param("status") ApprovalStatus status);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role ORDER BY u.createdAt DESC")
    List<User> findRecentByRole(@Param("role") RoleEnum role, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r = :role AND u.createdAt BETWEEN :start AND :end")
    Long countByRoleAndDateRange(@Param("role") RoleEnum role, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    boolean existsByEmailAndRolesContaining(String email, RoleEnum role);
    @Query("SELECT u.approvalStatus as status, COUNT(u) as count FROM User u JOIN u.roles r WHERE r = :role GROUP BY u.approvalStatus")
    List<ApprovalStatusCountProjection> getAgencyApprovalCounts(@Param("role") RoleEnum role);


    @Query("SELECT FUNCTION('DATE', u.createdAt) as date, COUNT(u) as count " +
            "FROM User u JOIN u.roles r " +
            "WHERE r = :role AND u.createdAt >= :startDate " +
            "GROUP BY FUNCTION('DATE', u.createdAt)")
    List<DailyAgencyCountProjection> getDailyAgencyCounts(@Param("role") RoleEnum role,
                                                          @Param("startDate") LocalDateTime startDate);

    @Query("SELECT " +
            "COUNT(u) as totalAgencies, " +
            "SUM(CASE WHEN u.approvalStatus = 'PENDING' THEN 1 ELSE 0 END) as pendingAgencies, " +
            "SUM(CASE WHEN u.approvalStatus = 'APPROVED' THEN 1 ELSE 0 END) as approvedAgencies, " +
            "SUM(CASE WHEN u.approvalStatus = 'REJECTED' THEN 1 ELSE 0 END) as rejectedAgencies " +
            "FROM User u JOIN u.roles r WHERE r = :role")
    AgencyAggregateStatsProjection getAgencyAggregateStats(@Param("role") RoleEnum role);
}