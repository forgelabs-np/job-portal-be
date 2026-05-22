package com.jobportal.v1.repository.projection;

public interface AgencyAggregateStatsProjection {
    Long getTotalAgencies();
    Long getPendingAgencies();
    Long getApprovedAgencies();
    Long getRejectedAgencies();
}