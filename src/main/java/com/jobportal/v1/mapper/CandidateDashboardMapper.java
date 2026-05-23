package com.jobportal.v1.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface CandidateDashboardMapper {

    // Get candidate basic info
    Map<String, Object> getCandidateInfo(@Param("userId") Long userId);

    // Document stats (single query)
    Map<String, Object> getDocumentStats(@Param("candidateId") Long candidateId);

    // Application status counts (GROUP BY)
    List<Map<String, Object>> getApplicationStatusCounts(@Param("candidateId") Long candidateId);

    // Count distinct applied jobs
    Long countDistinctAppliedJobs(@Param("candidateId") Long candidateId);

    // Recent applications with all needed fields (LIMIT 5)
    List<Map<String, Object>> getRecentApplications(@Param("candidateId") Long candidateId,
                                                    @Param("limit") int limit);

    // Recommended jobs - NOT EXISTS query (exactly 5)
    List<Map<String, Object>> getRecommendedJobs(@Param("candidateId") Long candidateId,
                                                 @Param("status") String status,
                                                 @Param("limit") int limit);

    // Weekly activity (last 7 days grouped)
    List<Map<String, Object>> getWeeklyActivity(@Param("candidateId") Long candidateId,
                                                @Param("startDate") LocalDateTime startDate);

    // Document list for summary
    List<Map<String, Object>> getDocumentSummary(@Param("candidateId") Long candidateId);

    // Total public jobs count
    Long getTotalPublicJobsCount(@Param("status") String status);
}