package com.llms.repository;

import com.llms.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByEntityAndEntityId(String entity, UUID entityId, Pageable pageable);

    Page<AuditLog> findByEntity(String entity, Pageable pageable);

    Page<AuditLog> findByPerformedById(UUID userId, Pageable pageable);
}
