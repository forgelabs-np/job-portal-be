package com.jobportal.v1.dto.agency.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AgencyWeeklyActivity {
    private List<String> days;
    private List<Long> applicationsSubmitted;
    private List<Long> applicationsShortlisted;
}
