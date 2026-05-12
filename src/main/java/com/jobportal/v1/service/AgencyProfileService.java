package com.jobportal.v1.service;

import com.jobportal.v1.dto.agency.request.AgencyProfileRequest;
import com.jobportal.v1.dto.agency.response.AgencyDashboardResponse;
import com.jobportal.v1.dto.agency.response.AgencyDocumentResponse;
import com.jobportal.v1.dto.agency.response.AgencyProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AgencyProfileService {

    // Profile CRUD
    AgencyProfileResponse createOrUpdateProfile(Long userId, AgencyProfileRequest request);

    AgencyProfileResponse getProfileByUserId(Long userId);

    AgencyProfileResponse getMyProfile(Long userId);

    boolean isProfileComplete(Long userId);

    // Document Management
    AgencyDocumentResponse uploadDocument(Long userId, String documentType, MultipartFile file);

    List<AgencyDocumentResponse> getMyDocuments(Long userId);

    void deleteDocument(Long userId, Long documentId);

    // Dashboard
    AgencyDashboardResponse getAgencyDashboard(Long agencyId);
}