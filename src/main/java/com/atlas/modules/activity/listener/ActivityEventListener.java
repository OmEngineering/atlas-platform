package com.atlas.modules.activity.listener;

import com.atlas.event.DomainEvent;
import com.atlas.modules.activity.entity.ActivityTargetType;
import com.atlas.modules.activity.service.ActivityService;
import com.atlas.modules.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ActivityEventListener {

    private static final Logger log = LoggerFactory.getLogger(ActivityEventListener.class);

    private final ActivityService activityService;
    private final UserRepository userRepository;

    public ActivityEventListener(ActivityService activityService, UserRepository userRepository) {
        this.activityService = activityService;
        this.userRepository = userRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainEvent(DomainEvent event) {
        try {
            switch (event.eventType()) {
                case "task.created" -> handleTaskCreated(event);
                case "task.status_changed" -> handleTaskStatusChanged(event);
                case "task.assignee_added" -> handleTaskAssigneeAdded(event);
                case "comment.created" -> handleCommentCreated(event);
                case "invitation.accepted" -> handleInvitationAccepted(event);
                default -> {
                    // Activity consumer ignores events it does not project yet.
                }
            }
        } catch (Exception ex) {
            log.error("Failed to record activity for eventType={} eventId={}",
                    event.eventType(), event.eventId(), ex);
        }
    }

    private void handleTaskCreated(DomainEvent event) {
        Map<String, Object> payload = event.payload();
        UUID taskId = uuid(payload.get("taskId"));
        String taskKey = string(payload.get("taskKey"));
        String title = string(payload.get("title"));
        String actorName = actorName(event.actorId());
        String summary = actorName + " created " + taskKey
                + (title.isBlank() ? "" : " — " + title);

        activityService.record(
                event.organizationId(),
                uuid(payload.get("projectId")),
                event.actorId(),
                event.eventType(),
                summary,
                ActivityTargetType.TASK,
                taskId,
                copyPayload(payload));
    }

    private void handleTaskStatusChanged(DomainEvent event) {
        Map<String, Object> payload = event.payload();
        UUID taskId = uuid(payload.get("taskId"));
        String taskKey = string(payload.get("taskKey"));
        String toStatus = string(payload.get("toStatus"));
        String actorName = actorName(event.actorId());
        String summary = actorName + " moved " + taskKey + " to " + humanStatus(toStatus);

        activityService.record(
                event.organizationId(),
                uuid(payload.get("projectId")),
                event.actorId(),
                event.eventType(),
                summary,
                ActivityTargetType.TASK,
                taskId,
                copyPayload(payload));
    }

    private void handleTaskAssigneeAdded(DomainEvent event) {
        Map<String, Object> payload = event.payload();
        UUID taskId = uuid(payload.get("taskId"));
        UUID assigneeId = uuid(payload.get("assigneeId"));
        String taskKey = string(payload.get("taskKey"));
        String actorName = actorName(event.actorId());
        String assigneeName = actorName(assigneeId);
        String summary = actorName + " assigned " + assigneeName + " to " + taskKey;

        activityService.record(
                event.organizationId(),
                uuid(payload.get("projectId")),
                event.actorId(),
                event.eventType(),
                summary,
                ActivityTargetType.TASK,
                taskId,
                copyPayload(payload));
    }

    private void handleCommentCreated(DomainEvent event) {
        Map<String, Object> payload = event.payload();
        UUID commentId = uuid(payload.get("commentId"));
        String taskKey = string(payload.get("taskKey"));
        String actorName = actorName(event.actorId());
        String onTask = taskKey.isBlank() ? "a task" : taskKey;
        String summary = actorName + " commented on " + onTask;

        activityService.record(
                event.organizationId(),
                uuid(payload.get("projectId")),
                event.actorId(),
                event.eventType(),
                summary,
                ActivityTargetType.COMMENT,
                commentId,
                copyPayload(payload));
    }

    private void handleInvitationAccepted(DomainEvent event) {
        Map<String, Object> payload = event.payload();
        UUID invitationId = uuid(payload.get("invitationId"));
        String memberName = string(payload.get("memberName"));
        if (memberName.isBlank()) {
            memberName = actorName(event.actorId());
        }
        String summary = memberName + " joined the organization";

        activityService.record(
                event.organizationId(),
                null,
                event.actorId(),
                event.eventType(),
                summary,
                ActivityTargetType.INVITATION,
                invitationId,
                copyPayload(payload));
    }

    private String actorName(UUID actorId) {
        if (actorId == null) {
            return "Someone";
        }
        return userRepository.findById(actorId)
                .map(user -> user.getFullName())
                .filter(name -> name != null && !name.isBlank())
                .orElse("Someone");
    }

    private String humanStatus(String status) {
        if (status == null || status.isBlank()) {
            return "updated";
        }
        return switch (status) {
            case "TODO" -> "To Do";
            case "IN_PROGRESS" -> "In Progress";
            case "IN_REVIEW" -> "In Review";
            case "DONE" -> "Done";
            case "CANCELLED" -> "Cancelled";
            default -> status;
        };
    }

    private Map<String, Object> copyPayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : new HashMap<>(payload);
    }

    private UUID uuid(Object value) {
        if (value instanceof UUID id) {
            return id;
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return UUID.fromString(text);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
