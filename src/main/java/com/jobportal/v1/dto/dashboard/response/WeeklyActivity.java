package com.jobportal.v1.dto.dashboard.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WeeklyActivity {
    private List<String> days;
    private List<Long> jobsCreated;
    private List<Long> agenciesJoined;
    private List<Long> candidatesSubmitted;
}
