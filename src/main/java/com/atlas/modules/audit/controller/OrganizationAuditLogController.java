package com.atlas.modules.audit.controller;

import com.atlas.modules.audit.entity.AuditTargetType;
import com.atlas.modules.audit.service.AuditLogService;
import com.atlas.modules.audit.dto.AuditLogResponse;
import com.atlas.shared.dto.ApiResponse;
import com.atlas.shared.dto.CursorPageResponse;
import com.atlas.shared.security.SecurityContextAccessor;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/audit-logs")
@Tag(name = "Audit Logs")
public class OrganizationAuditLogController {

    private final AuditLogService auditLogService;
    private final SecurityContextAccessor securityContextAccessor;

    public OrganizationAuditLogController(
            AuditLogService auditLogService,
            SecurityContextAccessor securityContextAccessor) {
        this.auditLogService = auditLogService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @GetMapping
    public ApiResponse<CursorPageResponse<AuditLogResponse>> list(
            @PathVariable UUID organizationId,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) UUID targetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int pageSize) {
        AuditTargetType parsedTargetType = null;
        if (targetType != null && !targetType.isBlank()) {
            parsedTargetType = AuditTargetType.valueOf(targetType.trim().toUpperCase());
        }
        return ApiResponse.of(auditLogService.listForOrganization(
                organizationId,
                securityContextAccessor.currentUserId(),
                actorId,
                parsedTargetType,
                targetId,
                from,
                to,
                cursor,
                pageSize));
    }
}
