package com.sharemyrecipe.dto;

import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        PaginationMeta pagination
) {
    public record PaginationMeta(
            int page,
            int pageSize,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {}
}
