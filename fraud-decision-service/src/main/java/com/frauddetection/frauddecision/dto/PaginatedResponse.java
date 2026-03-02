package com.frauddetection.frauddecision.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Stable JSON wrapper for paginated responses.
 *
 * Avoids direct serialization of framework Page implementations.
 */
public record PaginatedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <T> PaginatedResponse<T> from(Page<T> page) {
        return new PaginatedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}

