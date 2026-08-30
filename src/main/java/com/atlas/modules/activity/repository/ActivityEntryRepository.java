package com.atlas.modules.activity.repository;

import com.atlas.modules.activity.entity.ActivityEntry;
import com.atlas.modules.activity.entity.ActivityTargetType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ActivityEntryRepository extends JpaRepository<ActivityEntry, UUID> {

    @Query("""
            SELECT a FROM ActivityEntry a
            WHERE a.organizationId = :organizationId
              AND (:projectId IS NULL OR a.projectId = :projectId)
              AND (:targetType IS NULL OR a.targetType = :targetType)
              AND (:targetId IS NULL OR a.targetId = :targetId)
              AND (
                    :cursorCreatedAt IS NULL
                    OR a.createdAt < :cursorCreatedAt
                    OR (a.createdAt = :cursorCreatedAt AND a.id < :cursorId)
                  )
            ORDER BY a.createdAt DESC, a.id DESC
            """)
    List<ActivityEntry> findPage(
            @Param("organizationId") UUID organizationId,
            @Param("projectId") UUID projectId,
            @Param("targetType") ActivityTargetType targetType,
            @Param("targetId") UUID targetId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable);
}
