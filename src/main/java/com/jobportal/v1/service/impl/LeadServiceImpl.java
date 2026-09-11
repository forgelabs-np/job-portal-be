package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.lead.request.LeadRequest;
import com.jobportal.v1.dto.lead.response.LeadResponse;
import com.jobportal.v1.entity.Lead;
import com.jobportal.v1.enums.LeadSubject;
import com.jobportal.v1.exception.BadRequestException;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.LeadRepository;
import com.jobportal.v1.service.LeadHelper;
import com.jobportal.v1.service.LeadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final LeadHelper leadHelper;

    @Override
    @Transactional
    public LeadResponse submitLead(LeadRequest request) {
        log.info("Submitting new lead from: {}", request.getEmail());

        Lead lead = new Lead();
        lead.setFullName(request.getFullName());
        lead.setPhoneNumber(request.getPhoneNumber());
        lead.setEmail(request.getEmail());
        lead.setLocation(request.getLocation());
        lead.setSubject(request.getSubject());
        lead.setDescription(request.getDescription());
        lead.setIsRead(false);
        lead.setIsProcessed(false);

        Lead saved = leadRepository.save(lead);
        log.info("Lead submitted successfully with id: {}", saved.getId());

        return leadHelper.mapToResponse(saved);
    }

    @Override
    public LeadResponse getLeadById(Long id) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));
        return leadHelper.mapToResponse(lead);
    }

    @Override
    public Page<LeadResponse> getAllLeads(String fullName, String email, LeadSubject subject,
                                          Boolean isRead, Boolean isProcessed, Pageable pageable) {
        return leadRepository.findAllWithFilters(fullName, email, subject, isRead, isProcessed, pageable)
                .map(leadHelper::mapToResponse);
    }

    @Override
    @Transactional
    public LeadResponse updateLead(Long id, LeadRequest request) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));

        log.info("Updating lead: {}", id);

        lead.setFullName(request.getFullName());
        lead.setPhoneNumber(request.getPhoneNumber());
        lead.setEmail(request.getEmail());
        lead.setLocation(request.getLocation());
        lead.setSubject(request.getSubject());
        lead.setDescription(request.getDescription());

        Lead saved = leadRepository.save(lead);
        log.info("Lead updated successfully: {}", id);

        return leadHelper.mapToResponse(saved);
    }

    @Override
    @Transactional
    public LeadResponse markAsRead(Long id) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));

        lead.setIsRead(true);
        Lead saved = leadRepository.save(lead);
        log.info("Lead marked as read: {}", id);

        return leadHelper.mapToResponse(saved);
    }

    @Override
    @Transactional
    public LeadResponse markAsProcessed(Long id) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));

        lead.setIsProcessed(true);
        lead.setIsRead(true);
        Lead saved = leadRepository.save(lead);
        log.info("Lead marked as processed: {}", id);

        return leadHelper.mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteLead(Long id) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));

        leadRepository.delete(lead);
        log.info("Lead deleted: {}", id);
    }

    @Override
    public long getUnreadCount() {
        return leadRepository.countByIsReadFalse();
    }

    @Override
    public long getUnprocessedCount() {
        return leadRepository.countByIsProcessedFalse();
    }
}
