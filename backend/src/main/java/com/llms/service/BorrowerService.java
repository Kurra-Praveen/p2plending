package com.llms.service;

import com.llms.dto.request.CreateBorrowerRequest;
import com.llms.dto.request.UpdateBorrowerRequest;
import com.llms.dto.response.BorrowerResponse;
import com.llms.entity.Borrower;
import com.llms.entity.User;
import com.llms.enums.BorrowerStatus;
import com.llms.exception.DuplicateResourceException;
import com.llms.exception.ResourceNotFoundException;
import com.llms.repository.BorrowerRepository;
import com.llms.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BorrowerService {

    private final BorrowerRepository borrowerRepository;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    @Transactional
    public BorrowerResponse createBorrower(CreateBorrowerRequest request) {
        if (borrowerRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("Borrower with phone " + request.getPhone() + " already exists");
        }

        User currentUser = securityUtils.getCurrentUser();

        Borrower borrower = Borrower.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .riskScore(request.getRiskScore() != null ? request.getRiskScore() : 0)
                .createdBy(currentUser)
                .build();

        borrower = borrowerRepository.save(borrower);
        log.info("Borrower created: {} by user {}", borrower.getId(), currentUser.getEmail());

        auditService.logCreate("Borrower", borrower.getId(), borrower);

        return BorrowerResponse.from(borrower);
    }

    @Transactional(readOnly = true)
    public BorrowerResponse getBorrower(UUID id) {
        Borrower borrower;
        if (securityUtils.isAdminOrAuditor()) {
            // Admins and auditors can view all borrowers
            borrower = borrowerRepository.findByIdAndNotDeleted(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Borrower", "id", id));
        } else {
            UUID currentUserId = securityUtils.getCurrentUserId();
            borrower = borrowerRepository.findByIdAndCreatedByAndNotDeleted(id, currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Borrower", "id", id));
        }
        return BorrowerResponse.from(borrower);
    }

    @Transactional(readOnly = true)
    public Page<BorrowerResponse> getAllBorrowers(Pageable pageable) {
        if (securityUtils.isAdminOrAuditor()) {
            // Admins and auditors can view all borrowers
            return borrowerRepository.findAllNotDeleted(pageable)
                    .map(BorrowerResponse::from);
        }
        UUID currentUserId = securityUtils.getCurrentUserId();
        return borrowerRepository.findAllByCreatedByAndNotDeleted(currentUserId, pageable)
                .map(BorrowerResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<BorrowerResponse> getBorrowersByStatus(BorrowerStatus status, Pageable pageable) {
        if (securityUtils.isAdminOrAuditor()) {
            // Admins and auditors can view all borrowers
            return borrowerRepository.findByStatusAndNotDeleted(status, pageable)
                    .map(BorrowerResponse::from);
        }
        UUID currentUserId = securityUtils.getCurrentUserId();
        return borrowerRepository.findByStatusAndCreatedByAndNotDeleted(status, currentUserId, pageable)
                .map(BorrowerResponse::from);
    }

    @Transactional
    public BorrowerResponse updateBorrower(UUID id, UpdateBorrowerRequest request) {
        Borrower borrower = findBorrowerOrThrow(id);
        Borrower beforeState = copyBorrower(borrower);

        if (request.getFullName() != null) {
            borrower.setFullName(request.getFullName());
        }
        if (request.getEmail() != null) {
            borrower.setEmail(request.getEmail());
        }
        if (request.getAddress() != null) {
            borrower.setAddress(request.getAddress());
        }
        if (request.getRiskScore() != null) {
            borrower.setRiskScore(request.getRiskScore());
        }

        borrower = borrowerRepository.save(borrower);
        log.info("Borrower updated: {}", borrower.getId());

        auditService.logUpdate("Borrower", borrower.getId(), beforeState, borrower);

        return BorrowerResponse.from(borrower);
    }

    @Transactional
    public void blockBorrower(UUID id) {
        Borrower borrower = findBorrowerOrThrow(id);
        Borrower beforeState = copyBorrower(borrower);

        borrower.setStatus(BorrowerStatus.BLOCKED);
        borrowerRepository.save(borrower);

        log.info("Borrower blocked: {}", id);
        auditService.logStatusChange("Borrower", borrower.getId(), beforeState, borrower);
    }

    @Transactional
    public void unblockBorrower(UUID id) {
        Borrower borrower = findBorrowerOrThrow(id);
        Borrower beforeState = copyBorrower(borrower);

        borrower.setStatus(BorrowerStatus.ACTIVE);
        borrowerRepository.save(borrower);

        log.info("Borrower unblocked: {}", id);
        auditService.logStatusChange("Borrower", borrower.getId(), beforeState, borrower);
    }

    @Transactional
    public void deleteBorrower(UUID id) {
        Borrower borrower = findBorrowerOrThrow(id);

        borrower.setDeletedAt(LocalDateTime.now());
        borrowerRepository.save(borrower);

        log.info("Borrower soft deleted: {}", id);
        auditService.logDelete("Borrower", borrower.getId(), borrower);
    }

    private Borrower findBorrowerOrThrow(UUID id) {
        if (securityUtils.isAdminOrAuditor()) {
            // Admins and auditors can access all borrowers
            return borrowerRepository.findByIdAndNotDeleted(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Borrower", "id", id));
        }
        UUID currentUserId = securityUtils.getCurrentUserId();
        return borrowerRepository.findByIdAndCreatedByAndNotDeleted(id, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower", "id", id));
    }

    private Borrower copyBorrower(Borrower borrower) {
        return Borrower.builder()
                .id(borrower.getId())
                .fullName(borrower.getFullName())
                .phone(borrower.getPhone())
                .email(borrower.getEmail())
                .address(borrower.getAddress())
                .riskScore(borrower.getRiskScore())
                .status(borrower.getStatus())
                .build();
    }
}
