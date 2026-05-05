package com.jobportal.v1.repository;

import com.jobportal.v1.entity.AgencyProfile;
import com.jobportal.v1.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgencyProfileRepository extends JpaRepository<AgencyProfile, Long> {
    Optional<AgencyProfile> findByUser(User user);
    Optional<AgencyProfile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}