package com.jobportal.v1.dto.agency.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jobportal.v1.enums.ApprovalStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AgencyApprovalResponse {
    private Long userId;
    private String email;
    private ApprovalStatus approvalStatus;
    private String rejectionReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvedAt;

    private String message;
}