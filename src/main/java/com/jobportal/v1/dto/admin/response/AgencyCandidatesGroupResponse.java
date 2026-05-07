package com.jobportal.v1.dto.admin.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AgencyCandidatesGroupResponse {
    private Long agencyId;
    private String agencyName;
    private String agencyEmail;
    private List<CandidateInfo> candidates;
}
