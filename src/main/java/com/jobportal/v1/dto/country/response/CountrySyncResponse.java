package com.jobportal.v1.dto.country.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CountrySyncResponse {
    private Boolean success;
    private String message;
    private Integer addedCount;
    private Integer updatedCount;
    private Integer totalCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}