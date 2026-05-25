package com.gabaritaplus.api.dto.common;

public record PageMetadata(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
