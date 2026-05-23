package com.jobportal.v1.repository.projection;

public interface DailyJobCountProjection {
    String getDate();

    Long getCount();
}
