package com.atlas.modules.labels.controller;

import com.atlas.modules.labels.service.LabelService;
import com.atlas.shared.security.SecurityContextAccessor;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/labels")
@Tag(name = "Labels")
public class LabelController {

    private final LabelService labelService;
    private final SecurityContextAccessor securityContextAccessor;

    public LabelController(LabelService labelService, SecurityContextAccessor securityContextAccessor) {
        this.labelService = labelService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        labelService.delete(id, securityContextAccessor.currentUserId());
    }
}
