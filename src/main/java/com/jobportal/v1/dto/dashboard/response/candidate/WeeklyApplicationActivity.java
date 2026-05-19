package com.jobportal.v1.dto.dashboard.response.candidate;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class WeeklyApplicationActivity {
    private List<String> days;
    private List<Long> applicationsSubmitted;
}