package com.atlas.modules.audit.repository;

import com.atlas.modules.audit.entity.AuditLog;
import com.atlas.modules.audit.entity.AuditTargetType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.organizationId = :organizationId
              AND (:actorId IS NULL OR a.actorId = :actorId)
              AND (:targetType IS NULL OR a.targetType = :targetType)
              AND (:targetId IS NULL OR a.targetId = :targetId)
              AND (:fromDate IS NULL OR a.createdAt >= :fromDate)
              AND (:toDate IS NULL OR a.createdAt <= :toDate)
              AND (
                    :cursorCreatedAt IS NULL
                    OR a.createdAt < :cursorCreatedAt
                    OR (a.createdAt = :cursorCreatedAt AND a.id < :cursorId)
                  )
            ORDER BY a.createdAt DESC, a.id DESC
            """)
    List<AuditLog> findPage(
            @Param("organizationId") UUID organizationId,
            @Param("actorId") UUID actorId,
            @Param("targetType") AuditTargetType targetType,
            @Param("targetId") UUID targetId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable);
}
