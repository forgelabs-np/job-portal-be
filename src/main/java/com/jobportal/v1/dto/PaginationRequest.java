package com.jobportal.v1.dto;

import lombok.Data;

@Data
public class PaginationRequest {
    private int page = 0;
    private int size = 20;
}