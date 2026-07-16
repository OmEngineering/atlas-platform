package com.atlas.modules.notifications.listener;

import com.atlas.event.DomainEvent;
import com.atlas.modules.notifications.entity.NotificationTargetType;
import com.atlas.modules.notifications.service.NotificationDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationDispatcher notificationDispatcher;

    public NotificationEventListener(NotificationDispatcher notificationDispatcher) {
        this.notificationDispatcher = notificationDispatcher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainEvent(DomainEvent event) {
        try {
            switch (event.eventType()) {
                case "task.status_changed" -> handleTaskStatusChanged(event);
                case "task.assignee_added" -> handleTaskAssigneeAdded(event);
                case "comment.created" -> handleCommentCreated(event);
                case "invitation.accepted" -> handleInvitationAccepted(event);
                default -> {
                    // Other catalog events are ignored by the notification consumer in v1.
                }
            }
        } catch (Exception ex) {
            log.error("Failed to process notification for eventType={} eventId={}",
                    event.eventType(), event.eventId(), ex);
        }
    }

    private void handleTaskStatusChanged(DomainEvent event) {
        Map<String, Object> payload = event.payload();
        UUID taskId = uuid(payload.get("taskId"));
        String taskKey = string(payload.get("taskKey"));
        String toStatus = string(payload.get("toStatus"));
        Set<UUID> recipients = new LinkedHashSet<>();
        recipients.addAll(uuidList(payload.get("assigneeIds")));
        UUID createdBy = uuid(payload.get("createdBy"));
        if (createdBy != null) {
            recipients.add(createdBy);
        }
        recipients.remove(event.actorId());

        String title = "Task status updated";
        String body = taskKey + " moved to " + toStatus;
        for (UUID recipient : recipients) {
            notificationDispatcher.dispatch(
                    recipient,
                    event.organizationId(),
                    event.eventType(),
                    title,
                    body,
                    NotificationTargetType.TASK,
                    taskId);
        }
    }

    private void handleTaskAssigneeAdded(DomainEvent event) {
        Map<String, Object> payload = event.payload();
        UUID taskId = uuid(payload.get("taskId"));
        UUID assigneeId = uuid(payload.get("assigneeId"));
        String taskKey = string(payload.get("taskKey"));
        if (assigneeId == null || assigneeId.equals(event.actorId())) {
            return;
        }
        notificationDispatcher.dispatch(
                assigneeId,
                event.organizationId(),
                event.eventType(),
                "You were assigned a task",
                "You were assigned to " + taskKey,
                NotificationTargetType.TASK,
                taskId);
    }

    private void handleCommentCreated(DomainEvent event) {
        Map<String, Object> payload = event.payload();
        UUID commentId = uuid(payload.get("commentId"));
        Set<UUID> mentionedUserIds = new LinkedHashSet<>(uuidList(payload.get("mentionedUserIds")));
        mentionedUserIds.remove(event.actorId());
        String preview = string(payload.get("bodyPreview"));
        for (UUID recipient : mentionedUserIds) {
            notificationDispatcher.dispatch(
                    recipient,
                    event.organizationId(),
                    "comment.mentioned",
                    "You were mentioned in a comment",
                    preview,
                    NotificationTargetType.COMMENT,
                    commentId);
        }
    }

    private void handleInvitationAccepted(DomainEvent event) {
        Map<String, Object> payload = event.payload();
        UUID invitationId = uuid(payload.get("invitationId"));
        UUID inviterId = uuid(payload.get("inviterId"));
        String memberName = string(payload.get("memberName"));
        if (inviterId == null || inviterId.equals(event.actorId())) {
            return;
        }
        notificationDispatcher.dispatch(
                inviterId,
                event.organizationId(),
                event.eventType(),
                "Invitation accepted",
                memberName + " joined your organization",
                NotificationTargetType.INVITATION,
                invitationId);
    }

    @SuppressWarnings("unchecked")
    private Collection<UUID> uuidList(Object value) {
        if (value instanceof Collection<?> collection) {
            Set<UUID> ids = new LinkedHashSet<>();
            for (Object item : collection) {
                UUID id = uuid(item);
                if (id != null) {
                    ids.add(id);
                }
            }
            return ids;
        }
        return Set.of();
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
