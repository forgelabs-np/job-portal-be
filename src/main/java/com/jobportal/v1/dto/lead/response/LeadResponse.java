package com.jobportal.v1.dto.lead.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jobportal.v1.enums.LeadSubject;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Lead response")
public class LeadResponse {

    @Schema(description = "Lead ID")
    private Long id;

    @Schema(description = "Full name")
    private String fullName;

    @Schema(description = "Phone number")
    private String phoneNumber;

    @Schema(description = "Email address")
    private String email;

    @Schema(description = "Location")
    private String location;

    @Schema(description = "Subject of inquiry")
    private LeadSubject subject;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Whether the lead has been read")
    private Boolean isRead;

    @Schema(description = "Whether the lead has been processed")
    private Boolean isProcessed;

    @Schema(description = "Creation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
