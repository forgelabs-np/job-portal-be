package com.jobportal.v1.repository.projection;

public interface CandidateAggregateStatsProjection {
    Long getTotalSelfCandidates();
    Long getActiveSelfCandidates();
    Long getInactiveSelfCandidates();
    Long getCompleteProfileSelfCandidates();
}