package com.atlas.modules.audit.service;

import com.atlas.exception.ApiException;
import com.atlas.exception.ErrorCode;
import com.atlas.modules.audit.dto.AuditLogResponse;
import com.atlas.modules.audit.entity.AuditActorType;
import com.atlas.modules.audit.entity.AuditLog;
import com.atlas.modules.audit.entity.AuditTargetType;
import com.atlas.modules.audit.mapper.AuditLogMapper;
import com.atlas.modules.audit.repository.AuditLogRepository;
import com.atlas.modules.organizations.entity.MembershipRole;
import com.atlas.modules.organizations.service.MembershipAuthorizationService;
import com.atlas.shared.dto.CursorPageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final MembershipAuthorizationService membershipAuthorizationService;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            MembershipAuthorizationService membershipAuthorizationService) {
        this.auditLogRepository = auditLogRepository;
        this.membershipAuthorizationService = membershipAuthorizationService;
    }

    @Transactional
    public void record(
            UUID organizationId,
            UUID actorId,
            AuditActorType actorType,
            String action,
            AuditTargetType targetType,
            UUID targetId,
            Map<String, Object> metadata) {
        AuditLog log = new AuditLog();
        log.setOrganizationId(organizationId);
        log.setActorId(actorId);
        log.setActorType(actorType);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setMetadata(metadata == null ? Map.of() : new HashMap<>(metadata));
        log.setCreatedAt(Instant.now());
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<AuditLogResponse> listForOrganization(
            UUID organizationId,
            UUID userId,
            UUID actorId,
            AuditTargetType targetType,
            UUID targetId,
            Instant fromDate,
            Instant toDate,
            String cursor,
            int pageSize) {
        membershipAuthorizationService.requireActiveMembership(organizationId, userId, MembershipRole.ADMIN);

        int limit = Math.min(Math.max(pageSize, 1), 100);
        Instant cursorCreatedAt = null;
        UUID cursorId = null;
        if (cursor != null && !cursor.isBlank()) {
            String[] parts = cursor.split("\\|", 2);
            if (parts.length != 2) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid cursor", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            try {
                cursorCreatedAt = Instant.parse(parts[0]);
                cursorId = UUID.fromString(parts[1]);
            } catch (Exception ex) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid cursor", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        List<AuditLog> rows = auditLogRepository.findPage(
                organizationId,
                actorId,
                targetType,
                targetId,
                fromDate,
                toDate,
                cursorCreatedAt,
                cursorId,
                PageRequest.of(0, limit + 1));

        boolean hasMore = rows.size() > limit;
        List<AuditLog> page = hasMore ? rows.subList(0, limit) : rows;
        List<AuditLogResponse> items = page.stream().map(AuditLogMapper::toResponse).toList();

        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            AuditLog last = page.get(page.size() - 1);
            nextCursor = last.getCreatedAt().toString() + "|" + last.getId();
        }

        return new CursorPageResponse<>(items, nextCursor, hasMore);
    }
}
