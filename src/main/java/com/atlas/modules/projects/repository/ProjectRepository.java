package com.atlas.modules.projects.repository;

import com.atlas.modules.projects.entity.Project;
import com.atlas.modules.projects.entity.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    boolean existsByOrganizationIdAndKeyIgnoreCase(UUID organizationId, String key);

    Optional<Project> findByIdAndDeletedAtIsNull(UUID id);

    List<Project> findByOrganizationIdAndStatusAndDeletedAtIsNull(UUID organizationId, ProjectStatus status);

    @Query("""
            SELECT p FROM Project p
            WHERE p.organizationId = :organizationId
              AND p.status = :status
              AND p.deletedAt IS NULL
              AND (p.visibility = com.atlas.modules.projects.entity.ProjectVisibility.ORG_WIDE
                   OR p.id IN (
                       SELECT pm.projectId FROM ProjectMembership pm WHERE pm.userId = :userId
                   ))
            ORDER BY p.createdAt DESC
            """)
    List<Project> findVisibleForUser(
            @Param("organizationId") UUID organizationId,
            @Param("userId") UUID userId,
            @Param("status") ProjectStatus status);
}
