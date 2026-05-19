package com.jobportal.v1.service;


import com.jobportal.v1.dto.dashboard.response.candidate.CandidateDashboardResponse;

public interface CandidateDashboardService {
    CandidateDashboardResponse getCandidateDashboard(Long userId);
}