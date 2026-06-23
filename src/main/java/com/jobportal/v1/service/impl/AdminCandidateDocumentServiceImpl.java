package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.admin.request.DocumentApprovalRequest;
import com.jobportal.v1.dto.candidate.response.CandidateDocumentResponse;
import com.jobportal.v1.dto.candidate.response.DocumentVerificationStats;
import com.jobportal.v1.entity.Candidate;
import com.jobportal.v1.entity.CandidateDocument;
import com.jobportal.v1.enums.ApprovalStatus;
import com.jobportal.v1.enums.NotificationType;
import com.jobportal.v1.exception.BadRequestException;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.CandidateDocumentRepository;
import com.jobportal.v1.service.AdminCandidateDocumentService;
import com.jobportal.v1.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCandidateDocumentServiceImpl implements AdminCandidateDocumentService {

    private final CandidateDocumentRepository documentRepository;
    private final NotificationService notificationService;

    @Override
    public Page<CandidateDocumentResponse> getPendingDocuments(Pageable pageable) {
        return documentRepository.findByStatus(ApprovalStatus.PENDING, pageable)
                .map(this::toDocumentResponse);
    }

    @Override
    public List<CandidateDocumentResponse> getDocumentsByCandidate(Long candidateId) {
        return documentRepository.findByCandidateId(candidateId).stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CandidateDocumentResponse processDocumentApproval(DocumentApprovalRequest request, Long adminId) {

        CandidateDocument document = documentRepository.findByIdAndCandidateId(
                        request.getDocumentId(), request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found with id: " + request.getDocumentId() +
                                " for candidate: " + request.getCandidateId()));

        if (request.getStatus() == ApprovalStatus.APPROVED) {
            document.setStatus(ApprovalStatus.APPROVED);
            document.setRejectionReason(null);
            document.setVerifiedBy(adminId);
            document.setVerifiedAt(java.time.LocalDateTime.now());
            log.info("Document approved: {} by admin: {}", document.getId(), adminId);

            // ✅ Send notification ONLY if candidate has a user (self-registered)
            Candidate candidate = document.getCandidate();
            if (candidate.getUser() != null) {
                notificationService.sendNotification(
                        candidate.getUser().getId(),
                        NotificationType.DOCUMENT_APPROVED,
                        "Document Approved",
                        "Your " + document.getDocumentType() + " has been approved.",
                        "/candidate/documents"
                );
            }

        } else if (request.getStatus() == ApprovalStatus.REJECTED) {
            if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
                throw new BadRequestException("Rejection reason is required when rejecting a document");
            }
            document.setStatus(ApprovalStatus.REJECTED);
            document.setRejectionReason(request.getRejectionReason());
            document.setVerifiedBy(adminId);
            document.setVerifiedAt(java.time.LocalDateTime.now());
            log.info("Document rejected: {} by admin: {}", document.getId(), adminId);

            // ✅ Send notification ONLY if candidate has a user (self-registered)
            Candidate candidate = document.getCandidate();
            if (candidate.getUser() != null) {
                notificationService.sendNotification(
                        candidate.getUser().getId(),
                        NotificationType.DOCUMENT_REJECTED,
                        "Document Rejected",
                        "Your " + document.getDocumentType() + " was rejected: " + request.getRejectionReason(),
                        "/candidate/documents"
                );
            }

        } else {
            throw new BadRequestException("Invalid status. Only APPROVED or REJECTED are allowed.");
        }

        CandidateDocument saved = documentRepository.save(document);
        return toDocumentResponse(saved);
    }

    @Override
    public DocumentVerificationStats getDocumentStatistics() {
        long totalPending = documentRepository.countByStatus(ApprovalStatus.PENDING);
        long totalApproved = documentRepository.countByStatus(ApprovalStatus.APPROVED);
        long totalRejected = documentRepository.countByStatus(ApprovalStatus.REJECTED);
        long totalDocuments = documentRepository.count();

        double approvalRate = totalDocuments > 0 ? (double) totalApproved / totalDocuments * 100 : 0;

        return DocumentVerificationStats.builder()
                .totalDocuments(totalDocuments)
                .pending(totalPending)
                .approved(totalApproved)
                .rejected(totalRejected)
                .approvalRate(Math.round(approvalRate * 100.0) / 100.0)
                .build();
    }

    private CandidateDocumentResponse toDocumentResponse(CandidateDocument document) {
        return CandidateDocumentResponse.builder()
                .id(document.getId())
                .documentType(document.getDocumentType().name())
                .documentName(document.getDocumentName())
                .documentPath(document.getDocumentPath())
                .notes(document.getNotes())
                .uploadedAt(document.getUploadedAt() != null ?
                        document.getUploadedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                .status(document.getStatus() != null ? document.getStatus().name() : "PENDING")
                .rejectionReason(document.getRejectionReason())
                .build();
    }
}