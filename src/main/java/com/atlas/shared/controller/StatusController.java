package com.atlas.shared.controller;

import com.atlas.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/status")
@Tag(name = "Status", description = "Platform health and readiness probes")
public class StatusController {

    @GetMapping
    @Operation(summary = "Get platform status", description = "Returns a lightweight readiness payload for smoke tests.")
    public ApiResponse<Map<String, String>> status() {
        return ApiResponse.of(Map.of("status", "UP", "service", "atlas-platform"));
    }
}
