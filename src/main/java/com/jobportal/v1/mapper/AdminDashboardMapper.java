package com.jobportal.v1.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AdminDashboardMapper {

    // Job aggregate stats (total jobs, total slots, filled slots)
    Map<String, Object> getJobAggregateStats();

    // Candidate aggregate stats
    Map<String, Object> getCandidateAggregateStats(@Param("candidateType") String candidateType);

    // Agency aggregate stats
    Map<String, Object> getAgencyAggregateStats(@Param("role") String role);

    // Job status distribution (GROUP BY status)
    List<Map<String, Object>> getJobStatusCounts();

    // Recent jobs with country (LIMIT 5)
    List<Map<String, Object>> getRecentJobs(@Param("limit") int limit);

    // Recent agencies (LIMIT 5)
    List<Map<String, Object>> getRecentAgencies(@Param("role") String role, @Param("limit") int limit);

    // Daily job creation counts (last 7 days)
    List<Map<String, Object>> getDailyJobCounts(@Param("startDate") LocalDateTime startDate);

    // Daily agency joining counts (last 7 days)
    List<Map<String, Object>> getDailyAgencyCounts(@Param("role") String role, @Param("startDate") LocalDateTime startDate);
}