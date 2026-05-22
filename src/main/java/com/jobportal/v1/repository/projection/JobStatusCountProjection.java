package com.jobportal.v1.repository.projection;

public interface JobStatusCountProjection {
    String getStatus();
    Long getCount();
}