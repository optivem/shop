package com.mycompany.myshop.backend.usecases.queries.order;


import java.util.List;

// One page of order history, exactly as it goes on the wire.
//
// The rows are BrowseOrderHistoryItemResponse itself -- the projection the port returned -- not a copy of it. On the
// read side the projection IS the response: a query reports stored columns, so anything sitting
// between the port and the wire could only ever be a field-for-field transcription of the same
// names. The envelope exists only because the paging metadata has nowhere else to sit.
//
// page and size are the ones that were served, not the ones that were asked for. They differ when
// the request left them out.
//
// totalElements and totalPages are the two numbers a paged UI is built from: how many rows match,
// and therefore how many buttons to draw. totalPages is carried rather than left to the client to
// divide, so that every client rounds the same way.
public record BrowseOrderHistoryResponse(
        List<BrowseOrderHistoryItemResponse> orders,
        int page,
        int size,
        long totalElements,
        int totalPages) { }
