package com.jobportal.v1.repository.projection;

public interface JobAggregateStatsProjection {
    Long getTotalJobs();
    Long getTotalSlots();
    Long getFilledSlots();
}