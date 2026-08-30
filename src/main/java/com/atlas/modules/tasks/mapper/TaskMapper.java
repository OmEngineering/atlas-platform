package com.atlas.modules.tasks.mapper;

import com.atlas.modules.tasks.dto.TaskResponse;
import com.atlas.modules.tasks.entity.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TaskMapper {

    private TaskMapper() {
    }

    public static TaskResponse toResponse(Task task, long commentsCount, long attachmentsCount) {
        List<UUID> assigneeIds = new ArrayList<>(task.getAssigneeIds());
        List<UUID> labelIds = new ArrayList<>(task.getLabelIds());
        return new TaskResponse(
                task.getId(),
                task.getProjectId(),
                task.getMilestoneId(),
                task.getKey(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getPriority().name(),
                task.getDueDate(),
                task.getCompletedAt(),
                task.getCreatedBy(),
                assigneeIds,
                labelIds,
                commentsCount,
                attachmentsCount,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
