package com.jobportal.v1.service;

import com.jobportal.v1.dto.lead.request.LeadRequest;
import com.jobportal.v1.dto.lead.response.LeadResponse;
import com.jobportal.v1.enums.LeadSubject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LeadService {

    /**
     * Submit a new lead from the public landing page (no auth required).
     */
    LeadResponse submitLead(LeadRequest request);

    /**
     * Get a lead by ID (admin/staff only).
     */
    LeadResponse getLeadById(Long id);

    /**
     * Get all leads with filters and pagination (admin/staff only).
     */
    Page<LeadResponse> getAllLeads(String fullName, String email, LeadSubject subject,
                                   Boolean isRead, Boolean isProcessed, Pageable pageable);

    /**
     * Update a lead (admin/staff only).
     */
    LeadResponse updateLead(Long id, LeadRequest request);

    /**
     * Mark a lead as read (admin/staff only).
     */
    LeadResponse markAsRead(Long id);

    /**
     * Mark a lead as processed (admin/staff only).
     */
    LeadResponse markAsProcessed(Long id);

    /**
     * Delete a lead (admin/staff only).
     */
    void deleteLead(Long id);

    /**
     * Get unread lead count.
     */
    long getUnreadCount();

    /**
     * Get unprocessed lead count.
     */
    long getUnprocessedCount();
}
