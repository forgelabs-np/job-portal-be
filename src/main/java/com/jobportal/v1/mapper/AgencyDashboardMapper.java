package com.jobportal.v1.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AgencyDashboardMapper {

    // Candidate stats
    Map<String, Object> getCandidateStats(@Param("agencyId") Long agencyId);

    // Application status counts (GROUP BY)
    List<Map<String, Object>> getApplicationStatusCounts(@Param("agencyId") Long agencyId);

    // Assigned jobs stats
    Map<String, Object> getAssignedJobsStats(@Param("agencyId") Long agencyId);

    // Recent applications (LIMIT 5)
    List<Map<String, Object>> getRecentApplications(@Param("agencyId") Long agencyId,
                                                    @Param("limit") int limit);

    // Recent candidates (LIMIT 5)
    List<Map<String, Object>> getRecentCandidates(@Param("agencyId") Long agencyId,
                                                  @Param("limit") int limit);

    // Recent jobs (LIMIT 5)
    List<Map<String, Object>> getRecentJobs(@Param("agencyId") Long agencyId,
                                            @Param("limit") int limit);

    // Weekly submitted counts
    List<Map<String, Object>> getWeeklySubmittedCounts(@Param("agencyId") Long agencyId,
                                                       @Param("startDate") LocalDateTime startDate);

    // Weekly shortlisted counts
    List<Map<String, Object>> getWeeklyShortlistedCounts(@Param("agencyId") Long agencyId,
                                                         @Param("startDate") LocalDateTime startDate);
}