package com.newwave.student_management.common.util;

import org.springframework.data.domain.Page;

/**
 * Utility class for pagination operations.
 * Helps avoid code duplication when building paginated responses.
 */
public class PaginationUtil {

    /**
     * Pagination metadata container.
     */
    public static class PaginationMetadata {
        public final int page;
        public final int size;
        public final long totalElements;
        public final int totalPages;

        public PaginationMetadata(int page, int size, long totalElements, int totalPages) {
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }
    }

    /**
     * Extracts pagination metadata from a Page result.
     * 
     * @param pageResult The Spring Data Page result
     * @return PaginationMetadata containing page, size, totalElements, totalPages
     */
    public static <T> PaginationMetadata extractMetadata(Page<T> pageResult) {
        return new PaginationMetadata(
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    /**
     * Normalizes search string for SQL LIKE queries.
     * Escapes SQL wildcards (% and _) and wraps with % for case-insensitive search.
     * 
     * @param search The raw search string
     * @return Normalized search string (null if input is null or blank)
     */
    public static String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.trim().toLowerCase()
                .replace("%", "\\%")
                .replace("_", "\\_") + "%";
    }
}

