package com.atlas.modules.tasks.repository;

import com.atlas.modules.tasks.entity.Task;
import com.atlas.modules.tasks.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    long countByProjectId(UUID projectId);

    Optional<Task> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            SELECT t FROM Task t
            WHERE t.projectId = :projectId
              AND t.deletedAt IS NULL
              AND (:status IS NULL OR t.status = :status)
              AND (:milestoneId IS NULL OR t.milestoneId = :milestoneId)
              AND (:assigneeId IS NULL OR :assigneeId MEMBER OF t.assigneeIds)
              AND (:labelId IS NULL OR :labelId MEMBER OF t.labelIds)
            ORDER BY t.createdAt DESC
            """)
    List<Task> findByProjectWithFilters(
            @Param("projectId") UUID projectId,
            @Param("status") TaskStatus status,
            @Param("assigneeId") UUID assigneeId,
            @Param("labelId") UUID labelId,
            @Param("milestoneId") UUID milestoneId);
}
