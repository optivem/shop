package com.mycompany.myshop.backend.usecases.queries;

import java.util.List;
import java.util.Optional;

/**
 * One page of a list, and whether the database had more rows to give.
 *
 * <p>{@code hasMore} is answered by asking for one row more than the page size and seeing whether it
 * arrives. That is one row cheaper than a second {@code COUNT(*)} over the same predicate, and it is
 * the only extra fact a cursor-paged client needs: there is no total here, because a keyset page
 * never knows how many pages follow it -- and counting them would re-read everything the paging was
 * introduced to stop reading.
 *
 * <p>The next cursor is not held here. It is the sort key of {@link #last()}, and which columns make
 * up that key is the caller's business, not this record's.
 */
public record Page<T>(List<T> items, boolean hasMore) {

    public Optional<T> last() {
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(items.size() - 1));
    }
}
