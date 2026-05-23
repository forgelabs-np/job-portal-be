package com.jobportal.v1.repository.projection;

public interface DailyAgencyCountProjection {
    String getDate();

    Long getCount();
}