package com.atlas.shared.dto;

public record PageResponse<T>(
        java.util.List<T> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages
) {
}
