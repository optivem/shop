package com.mycompany.myshop.core.services;

import com.mycompany.myshop.core.exceptions.ValidationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Which page of a list to read. Pages are numbered from one; a missing page is the first one and a
 * missing size is {@link #DEFAULT_SIZE}. The size is capped at {@link #MAX_SIZE} so that no request
 * can ask for an unbounded read.
 */
public record PageSpec(int page, int size) {

    public static final int FIRST_PAGE = 1;
    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 100;

    private static final String FIELD_PAGE = "page";
    private static final String FIELD_SIZE = "size";

    /** Validates the optional query parameters and fills in the defaults. */
    public static PageSpec of(Integer page, Integer size) {
        if (page != null && page < FIRST_PAGE) {
            throw new ValidationException(FIELD_PAGE, "Page must be " + FIRST_PAGE + " or greater");
        }
        if (size != null && (size < 1 || size > MAX_SIZE)) {
            throw new ValidationException(FIELD_SIZE, "Page size must be between 1 and " + MAX_SIZE);
        }
        return new PageSpec(page == null ? FIRST_PAGE : page, size == null ? DEFAULT_SIZE : size);
    }

    /** Rows to skip. Long, because page is a number the caller chose and int * int overflows. */
    public long offset() {
        return (long) (page - FIRST_PAGE) * size;
    }

    /**
     * Whether this page can hold any of {@code totalElements} rows. A page past the end is not an
     * error, it is just empty; checking first also keeps a huge offset away from the database.
     */
    public boolean reaches(long totalElements) {
        return offset() < totalElements;
    }

    public Pageable toPageable() {
        return PageRequest.of(page - FIRST_PAGE, size);
    }

    public int totalPages(long totalElements) {
        return (int) ((totalElements + size - 1) / size);
    }
}
