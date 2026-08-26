package com.mycompany.myshop.backend.usecases.queries.order.ports;
import com.mycompany.myshop.backend.usecases.queries.order.BrowseOrderHistoryItemResponse;import com.mycompany.myshop.backend.usecases.queries.order.ViewOrderDetailsResponse;

import com.mycompany.myshop.backend.usecases.queries.common.Page;
import com.mycompany.myshop.backend.usecases.queries.common.PageSpec;
import java.util.Optional;

// The read side of orders.
//
// Both methods name an intent, not a mechanism. listOrders takes the optional filter and
// the page, so the LIKE and the LIMIT both stay in SQL instead of becoming an
// if and a subList in the use case.
//
// The before-picture is worth keeping in mind: this port used to be
// findAllByOrderByOrderTimestampDesc() on the domain repository -- Spring Data's
// query-derivation DSL spelled into a port whose own javadoc claimed "no Spring Data, no JPA".
// Because the port named a mechanism, "give me all the rows" was the only question it could be
// asked, so filtering and limiting had nowhere to happen but in memory.
//
// findOrderDetail is the projection twin of
// com.mycompany.myshop.backend.domain.repositories.OrderRepository#findByOrderNumber -- the
// same question, two answers. The command side needs the entity, because it is about to ask it to
// decide something; the query side needs the columns, because it is about to serialize them.
public interface OrderReader {

    // Newest first, one page at a time. A blank or null filter means "no filter"; anything
    // else is matched case-insensitively as a substring of the order number. The returned page
    // carries the total row count matching the filter, not just the rows on this page.
    Page<BrowseOrderHistoryItemResponse> listOrders(String orderNumberFilter, PageSpec page);

    Optional<ViewOrderDetailsResponse> findOrderDetail(String orderNumber);
}
